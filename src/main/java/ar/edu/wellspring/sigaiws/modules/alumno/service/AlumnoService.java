package ar.edu.wellspring.sigaiws.modules.alumno.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.DuplicateResourceException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.common.storage.AlmacenamientoService;
import ar.edu.wellspring.sigaiws.common.util.HashUtil;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoCicloLectivo;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoTutor;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.EstadoAlumno;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.DocumentoAlumno;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Tutor;
import ar.edu.wellspring.sigaiws.modules.alumno.dto.AlumnoForm;
import ar.edu.wellspring.sigaiws.modules.alumno.dto.TutorForm;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoCicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoTutorRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.DocumentoAlumnoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Modulo 5.1 (Gestion de Alumnos). Orquesta legajo, trayectoria anual
 * (AlumnoCicloLectivo) y tutores. La logica de condicion libre automatica
 * (seccion 5.1.6/5.2.3) vive en el modulo Asistencia (AsistenciaService),
 * que actualiza el flag AlumnoCicloLectivo.condicionLibre expuesto aqui.
 */
@Service
@RequiredArgsConstructor
public class AlumnoService {

    private static final String MODULO = "ALUMNOS";
    private static final int EDAD_MAXIMA = 25;
    private static final long FOTO_MAX_MB = 2;
    private static final long DOCUMENTO_MAX_MB = 5;

    private final AlumnoRepository alumnoRepository;
    private final AlumnoCicloLectivoRepository alumnoCicloLectivoRepository;
    private final AlumnoTutorRepository alumnoTutorRepository;
    private final DocumentoAlumnoRepository documentoAlumnoRepository;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final CursoDivisionRepository cursoDivisionRepository;
    private final TutorService tutorService;
    private final AlmacenamientoService almacenamientoService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<Alumno> buscar(String texto, EstadoAlumno estado, Long cursoDivisionId, Boolean condicionLibre,
                                Pageable pageable) {
        Long cicloActivoId = cicloLectivoRepository.findByActivoTrue().map(CicloLectivo::getId).orElse(null);
        return alumnoRepository.buscar(texto, estado, cursoDivisionId, condicionLibre, cicloActivoId, pageable);
    }

    /** Seccion 5.1.2: el Docente ve el listado filtrado a los cursos/divisiones donde tiene alguna materia asignada. */
    @Transactional(readOnly = true)
    public Page<Alumno> buscarParaDocente(String texto, EstadoAlumno estado, Long cursoDivisionId, Boolean condicionLibre,
                                           List<Long> cursosPermitidos, Pageable pageable) {
        if (cursosPermitidos.isEmpty()) {
            return Page.empty(pageable);
        }
        Long cicloActivoId = cicloLectivoRepository.findByActivoTrue().map(CicloLectivo::getId).orElse(null);
        return alumnoRepository.buscarEntreCursos(texto, estado, cursoDivisionId, condicionLibre, cicloActivoId, cursosPermitidos, pageable);
    }

    /** Defensa en profundidad: valida que el alumno curse hoy en alguno de los cursos indicados. */
    @Transactional(readOnly = true)
    public boolean estaEnCursos(Long alumnoId, List<Long> cursoIds) {
        try {
            return cursoIds.contains(cicloActualDe(alumnoId).getCursoDivision().getId());
        } catch (BusinessException e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public Alumno obtener(Long id) {
        return alumnoRepository.findById(id).orElseThrow(() -> NotFoundException.of("Alumno", id));
    }

    @Transactional(readOnly = true)
    public List<AlumnoCicloLectivo> trayectoria(Long alumnoId) {
        return alumnoCicloLectivoRepository.findAllByAlumno_IdOrderByCicloLectivo_AnioDesc(alumnoId);
    }

    /** Alumnos de una division en el ciclo lectivo activo (usado por Asistencia, Calificaciones, etc.). */
    @Transactional(readOnly = true)
    public List<Alumno> alumnosDeDivisionEnCicloActivo(Long cursoDivisionId) {
        CicloLectivo activo = cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));
        return alumnoCicloLectivoRepository
                .findAllByCursoDivision_IdAndCicloLectivo_IdOrderByAlumno_ApellidoAscAlumno_NombreAsc(cursoDivisionId, activo.getId())
                .stream()
                .map(AlumnoCicloLectivo::getAlumno)
                .toList();
    }

    @Transactional(readOnly = true)
    public AlumnoCicloLectivo cicloActualDe(Long alumnoId) {
        Long cicloActivoId = cicloLectivoRepository.findByActivoTrue()
                .map(CicloLectivo::getId)
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));
        return alumnoCicloLectivoRepository.findByAlumno_IdAndCicloLectivo_Id(alumnoId, cicloActivoId)
                .orElseThrow(() -> new BusinessException("El alumno no tiene trayectoria en el ciclo lectivo activo."));
    }

    @Transactional
    public Alumno crear(AlumnoForm form) {
        validarFechaNacimiento(form.getFechaNacimiento());
        String dniHash = HashUtil.sha256(form.getDni());
        if (alumnoRepository.existsByDniHash(dniHash)) {
            throw new DuplicateResourceException("dni", "Ya existe un alumno registrado con ese DNI.");
        }

        CicloLectivo cicloIngreso = cicloLectivoRepository.findById(form.getCicloLectivoIngresoId())
                .orElseThrow(() -> NotFoundException.of("Ciclo lectivo", form.getCicloLectivoIngresoId()));

        Alumno alumno = new Alumno();
        aplicarDatosPersonales(alumno, form, dniHash);
        alumno.setEstadoGlobal(form.getEstado());
        alumno.setCicloLectivoIngreso(cicloIngreso);
        alumno = alumnoRepository.save(alumno);

        AlumnoCicloLectivo acl = new AlumnoCicloLectivo();
        acl.setAlumno(alumno);
        acl.setCicloLectivo(cicloIngreso);
        acl.setCursoDivision(resolverCursoDivision(form.getAnio(), form.getDivision()));
        acl.setRepetidor(form.isRepetidor());
        alumnoCicloLectivoRepository.save(acl);

        vincularTutores(alumno, form.getTutores());

        auditService.registrar(MODULO, "CREAR", "Alumno", alumno.getId(), null, resumen(alumno));
        return alumno;
    }

    @Transactional
    public Alumno editar(Long id, AlumnoForm form) {
        Alumno alumno = obtener(id);
        String antes = resumen(alumno);

        validarFechaNacimiento(form.getFechaNacimiento());
        String dniHash = HashUtil.sha256(form.getDni());
        if (!alumno.getDniHash().equals(dniHash) && alumnoRepository.existsByDniHash(dniHash)) {
            throw new DuplicateResourceException("dni", "Ya existe un alumno registrado con ese DNI.");
        }

        aplicarDatosPersonales(alumno, form, dniHash);
        alumno.setEstadoGlobal(form.getEstado());
        alumno = alumnoRepository.save(alumno);

        AlumnoCicloLectivo acl = cicloActualDe(alumno.getId());
        acl.setCursoDivision(resolverCursoDivision(form.getAnio(), form.getDivision()));
        alumnoCicloLectivoRepository.save(acl);

        sincronizarTutores(alumno, form.getTutores());

        auditService.registrar(MODULO, "EDITAR", "Alumno", alumno.getId(), antes, resumen(alumno));
        return alumno;
    }

    /** Seccion 5.1.2: "Dar de baja / pase". Baja logica; el legajo se conserva completo. */
    @Transactional
    public void darDeBaja(Long id, String motivo) {
        Alumno alumno = obtener(id);
        alumno.setEstadoGlobal(EstadoAlumno.INACTIVO_BAJA);
        alumnoRepository.save(alumno);
        auditService.registrar(MODULO, "DAR_DE_BAJA", "Alumno", alumno.getId(), null, motivo);
    }

    /** Revierte una baja o un pase (IMPLEMENTACION.md 4.2, deshacer). */
    @Transactional
    public void reactivar(Long id) {
        Alumno alumno = obtener(id);
        alumno.setEstadoGlobal(EstadoAlumno.ACTIVO);
        alumnoRepository.save(alumno);
        auditService.registrar(MODULO, "DESHACER_BAJA_O_PASE", "Alumno", alumno.getId(), null, null);
    }

    /**
     * Seccion 5.1.7 (Flujo: Alumno Repetidor). Crea el registro de trayectoria del
     * nuevo ciclo lectivo vinculado al año anterior; el historial academico previo
     * queda intacto (append-only). El alumno arranca el nuevo ciclo con
     * inasistencias/disciplina en cero (valores por defecto de AlumnoCicloLectivo).
     */
    @Transactional
    public void registrarRepeticion(Long alumnoId, Long nuevoCursoDivisionId) {
        Alumno alumno = obtener(alumnoId);
        CicloLectivo cicloActivo = cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));

        if (alumnoCicloLectivoRepository.findByAlumno_IdAndCicloLectivo_Id(alumnoId, cicloActivo.getId()).isPresent()) {
            throw new BusinessException("El alumno ya tiene trayectoria registrada en el ciclo lectivo activo.");
        }

        AlumnoCicloLectivo acl = new AlumnoCicloLectivo();
        acl.setAlumno(alumno);
        acl.setCicloLectivo(cicloActivo);
        acl.setCursoDivision(cursoDivisionRepository.findById(nuevoCursoDivisionId)
                .orElseThrow(() -> NotFoundException.of("CursoDivision", nuevoCursoDivisionId)));
        acl.setRepetidor(true);
        alumnoCicloLectivoRepository.save(acl);

        auditService.registrar(MODULO, "REGISTRAR_REPETICION", "Alumno", alumno.getId(), null,
                "cicloLectivo=" + cicloActivo.getAnio());
    }

    @Transactional
    public void subirFoto(Long alumnoId, MultipartFile foto) {
        Alumno alumno = obtener(alumnoId);
        var guardado = almacenamientoService.guardar(foto, "alumnos/fotos", FOTO_MAX_MB);
        alumno.setFotoPath(guardado.pathRelativo());
        alumnoRepository.save(alumno);
        auditService.registrar(MODULO, "SUBIR_FOTO", "Alumno", alumno.getId(), null, null);
    }

    @Transactional
    public DocumentoAlumno subirDocumento(Long alumnoId, MultipartFile archivo, String tipo) {
        Alumno alumno = obtener(alumnoId);
        var guardado = almacenamientoService.guardar(archivo, "alumnos/documentos", DOCUMENTO_MAX_MB);

        DocumentoAlumno documento = new DocumentoAlumno();
        documento.setAlumno(alumno);
        documento.setTipo(tipo);
        documento.setNombreArchivo(guardado.nombreOriginal());
        documento.setPathArchivo(guardado.pathRelativo());
        documento.setTamanoBytes(guardado.tamanoBytes());
        documento = documentoAlumnoRepository.save(documento);

        auditService.registrar(MODULO, "SUBIR_DOCUMENTO", "DocumentoAlumno", documento.getId(), null, tipo);
        return documento;
    }

    @Transactional(readOnly = true)
    public List<DocumentoAlumno> documentosDe(Long alumnoId) {
        return documentoAlumnoRepository.findAllByAlumno_IdOrderBySubidoEnDesc(alumnoId);
    }

    @Transactional(readOnly = true)
    public List<AlumnoTutor> tutoresDe(Long alumnoId) {
        return alumnoTutorRepository.findAllByAlumno_Id(alumnoId);
    }

    /** Publico para que la importacion masiva valide con la misma regla antes de guardar. */
    public void validarFechaNacimiento(LocalDate fecha) {
        if (fecha.isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha de nacimiento no puede ser futura.");
        }
        int edad = Period.between(fecha, LocalDate.now()).getYears();
        if (edad > EDAD_MAXIMA) {
            throw new BusinessException("La fecha de nacimiento no puede corresponder a una edad mayor a " + EDAD_MAXIMA + " años.");
        }
    }

    private void aplicarDatosPersonales(Alumno alumno, AlumnoForm form, String dniHash) {
        alumno.setApellido(form.getApellido());
        alumno.setNombre(form.getNombre());
        alumno.setDni(form.getDni());
        alumno.setDniHash(dniHash);
        alumno.setFechaNacimiento(form.getFechaNacimiento());
        alumno.setGenero(form.getGenero());
        alumno.setNacionalidad(form.getNacionalidad());
        alumno.setDomicilio(form.getDomicilio());
        alumno.setLocalidad(form.getLocalidad());
    }

    private ar.edu.wellspring.sigaiws.modules.configuracion.domain.CursoDivision resolverCursoDivision(Integer anio, String division) {
        return cursoDivisionRepository.findByAnioAndDivision(anio, division)
                .orElseThrow(() -> new BusinessException("El año/division seleccionado no existe."));
    }

    private void vincularTutores(Alumno alumno, List<TutorForm> tutorForms) {
        for (TutorForm tf : tutorForms) {
            Tutor tutor = tutorService.resolverOCrear(tf);
            AlumnoTutor vinculo = new AlumnoTutor();
            vinculo.setAlumno(alumno);
            vinculo.setTutor(tutor);
            vinculo.setVinculo(tf.getVinculo());
            vinculo.setRecibeNotificaciones(tf.isRecibeNotificaciones());
            alumnoTutorRepository.save(vinculo);
        }
    }

    private void sincronizarTutores(Alumno alumno, List<TutorForm> tutorForms) {
        List<AlumnoTutor> actuales = alumnoTutorRepository.findAllByAlumno_Id(alumno.getId());
        Set<String> dniEnForm = tutorForms.stream().map(TutorForm::getDni).collect(Collectors.toSet());

        // Quita vinculos de tutores que ya no estan en el formulario.
        for (AlumnoTutor vinculo : actuales) {
            String dniHashActual = vinculo.getTutor().getDniHash();
            boolean sigueVinculado = dniEnForm.stream().anyMatch(dni -> HashUtil.sha256(dni).equals(dniHashActual));
            if (!sigueVinculado) {
                alumnoTutorRepository.delete(vinculo);
            }
        }

        // Crea/actualiza los tutores del formulario.
        for (TutorForm tf : tutorForms) {
            String dniHash = HashUtil.sha256(tf.getDni());
            Tutor tutor = tutorService.resolverOCrear(tf);
            AlumnoTutor vinculoExistente = actuales.stream()
                    .filter(v -> v.getTutor().getDniHash().equals(dniHash))
                    .findFirst().orElse(null);
            if (vinculoExistente == null) {
                AlumnoTutor vinculo = new AlumnoTutor();
                vinculo.setAlumno(alumno);
                vinculo.setTutor(tutor);
                vinculo.setVinculo(tf.getVinculo());
                vinculo.setRecibeNotificaciones(tf.isRecibeNotificaciones());
                alumnoTutorRepository.save(vinculo);
            } else {
                vinculoExistente.setVinculo(tf.getVinculo());
                vinculoExistente.setRecibeNotificaciones(tf.isRecibeNotificaciones());
                alumnoTutorRepository.save(vinculoExistente);
            }
        }
    }

    private String resumen(Alumno alumno) {
        return "apellido=%s, nombre=%s, estado=%s".formatted(alumno.getApellido(), alumno.getNombre(), alumno.getEstadoGlobal());
    }
}
