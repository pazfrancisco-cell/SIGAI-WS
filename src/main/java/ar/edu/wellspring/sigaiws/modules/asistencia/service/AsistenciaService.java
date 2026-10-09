package ar.edu.wellspring.sigaiws.modules.asistencia.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.common.notification.EmailService;
import ar.edu.wellspring.sigaiws.modules.mensajeria.service.NotificacionService;
import ar.edu.wellspring.sigaiws.modules.alerta.domain.Alerta;
import ar.edu.wellspring.sigaiws.modules.alerta.service.AlertaService;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoCicloLectivo;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoTutor;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoCicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoTutorRepository;
import ar.edu.wellspring.sigaiws.modules.asistencia.domain.AsistenciaClase;
import ar.edu.wellspring.sigaiws.modules.asistencia.domain.AsistenciaJornada;
import ar.edu.wellspring.sigaiws.modules.asistencia.domain.JustificacionInasistencia;
import ar.edu.wellspring.sigaiws.modules.asistencia.domain.TipoAsistenciaJornada;
import ar.edu.wellspring.sigaiws.modules.asistencia.dto.AsistenciaClaseBatchForm;
import ar.edu.wellspring.sigaiws.modules.asistencia.dto.AsistenciaJornadaBatchForm;
import ar.edu.wellspring.sigaiws.modules.asistencia.dto.JustificacionForm;
import ar.edu.wellspring.sigaiws.modules.asistencia.repository.AsistenciaClaseRepository;
import ar.edu.wellspring.sigaiws.modules.asistencia.repository.AsistenciaJornadaRepository;
import ar.edu.wellspring.sigaiws.modules.asistencia.repository.JustificacionInasistenciaRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.ParametroAcademico;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.MateriaRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.service.ParametroAcademicoService;
import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.modules.horario.repository.AsignacionDocenteRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Modulo 5.2 (Asistencia). Implementa la logica de umbrales de la seccion
 * 5.2.3 tal como fue interpretada en docs/DECISIONS.md ADR-008: el umbral de
 * Condicion Libre no es fijo en 28, depende de si al llegar a 20 faltas el
 * 50% o mas estaban justificadas.
 */
@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private static final String MODULO = "ASISTENCIA";
    private static final long DOCUMENTO_MAX_MB = 5;

    private final AsistenciaJornadaRepository asistenciaJornadaRepository;
    private final AsistenciaClaseRepository asistenciaClaseRepository;
    private final JustificacionInasistenciaRepository justificacionInasistenciaRepository;
    private final AlumnoRepository alumnoRepository;
    private final ar.edu.wellspring.sigaiws.modules.alumno.service.AlumnoService alumnoService;
    private final AlumnoCicloLectivoRepository alumnoCicloLectivoRepository;
    private final AlumnoTutorRepository alumnoTutorRepository;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final CursoDivisionRepository cursoDivisionRepository;
    private final MateriaRepository materiaRepository;
    private final ParametroAcademicoService parametroAcademicoService;
    private final AlertaService alertaService;
    private final EmailService emailService;
    private final NotificacionService notificacionService;
    private final AuditService auditService;
    private final ar.edu.wellspring.sigaiws.common.storage.AlmacenamientoService almacenamientoService;
    private final AsignacionDocenteRepository asignacionDocenteRepository;

    @Transactional(readOnly = true)
    public List<Alumno> alumnosDeDivision(Long cursoDivisionId) {
        return alumnoService.alumnosDeDivisionEnCicloActivo(cursoDivisionId);
    }

    /** Resumen de inasistencias acumuladas por alumno de una division, en el ciclo lectivo activo. */
    @Transactional(readOnly = true)
    public List<AlumnoCicloLectivo> resumenPorDivision(Long cursoDivisionId) {
        CicloLectivo activo = cicloActivo();
        return alumnoCicloLectivoRepository.findAllByCursoDivision_IdAndCicloLectivo_IdOrderByAlumno_ApellidoAscAlumno_NombreAsc(
                cursoDivisionId, activo.getId());
    }

    @Transactional
    public void registrarJornada(AsistenciaJornadaBatchForm form, Usuario registradoPor) {
        for (AsistenciaJornadaBatchForm.Item item : form.getItems()) {
            AsistenciaJornada registro = asistenciaJornadaRepository
                    .findByAlumno_IdAndFechaAndTurno(item.getAlumnoId(), form.getFecha(), form.getTurno())
                    .orElseGet(AsistenciaJornada::new);
            Alumno alumno = alumnoRepository.findById(item.getAlumnoId())
                    .orElseThrow(() -> NotFoundException.of("Alumno", item.getAlumnoId()));
            registro.setAlumno(alumno);
            registro.setFecha(form.getFecha());
            registro.setTurno(form.getTurno());
            registro.setTipo(item.getTipo());
            registro.setRegistradoPor(registradoPor);
            asistenciaJornadaRepository.save(registro);

            recalcularAcumulado(item.getAlumnoId());
        }
        auditService.registrar(MODULO, "REGISTRAR_ASISTENCIA_JORNADA", "AsistenciaJornada", null, null,
                "fecha=" + form.getFecha() + ", turno=" + form.getTurno() + ", alumnos=" + form.getItems().size());
    }

    @Transactional
    public void registrarClase(AsistenciaClaseBatchForm form, Usuario registradoPor) {
        var materia = materiaRepository.findById(form.getMateriaId())
                .orElseThrow(() -> NotFoundException.of("Materia", form.getMateriaId()));
        var cursoDivision = cursoDivisionRepository.findById(form.getCursoDivisionId())
                .orElseThrow(() -> NotFoundException.of("CursoDivision", form.getCursoDivisionId()));

        if (registradoPor.tieneRol(RolNombre.DOCENTE)) {
            CicloLectivo activo = cicloActivo();
            boolean asignado = asignacionDocenteRepository
                    .findAllByDocente_IdAndCicloLectivo_Id(registradoPor.getId(), activo.getId())
                    .stream()
                    .anyMatch(a -> a.getMateria().getId().equals(form.getMateriaId())
                            && a.getCursoDivision().getId().equals(form.getCursoDivisionId()));
            if (!asignado) {
                throw new BusinessException("No estas asignado a esa materia y curso/division.");
            }
        }

        for (AsistenciaClaseBatchForm.Item item : form.getItems()) {
            AsistenciaClase registro = asistenciaClaseRepository
                    .findByAlumno_IdAndMateria_IdAndFecha(item.getAlumnoId(), form.getMateriaId(), form.getFecha())
                    .orElseGet(AsistenciaClase::new);
            Alumno alumno = alumnoRepository.findById(item.getAlumnoId())
                    .orElseThrow(() -> NotFoundException.of("Alumno", item.getAlumnoId()));
            registro.setAlumno(alumno);
            registro.setMateria(materia);
            registro.setCursoDivision(cursoDivision);
            registro.setFecha(form.getFecha());
            registro.setTipo(item.getTipo());
            registro.setObservacion(item.getObservacion());
            registro.setRegistradoPor(registradoPor);
            asistenciaClaseRepository.save(registro);
        }
        auditService.registrar(MODULO, "REGISTRAR_ASISTENCIA_CLASE", "AsistenciaClase", null, null,
                "materia=" + materia.getNombre() + ", fecha=" + form.getFecha());
    }

    /** Seccion 5.2.4: % asistencia por materia = presentes / total de clases dictadas. */
    @Transactional(readOnly = true)
    public BigDecimal porcentajeAsistenciaMateria(Long alumnoId, Long materiaId) {
        long total = asistenciaClaseRepository.countByAlumno_IdAndMateria_Id(alumnoId, materiaId);
        if (total == 0) {
            return BigDecimal.valueOf(100);
        }
        long presentes = asistenciaClaseRepository.countByAlumno_IdAndMateria_IdAndTipo(
                alumnoId, materiaId, ar.edu.wellspring.sigaiws.modules.asistencia.domain.TipoAsistenciaClase.PRESENTE);
        return BigDecimal.valueOf(presentes).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, java.math.RoundingMode.HALF_UP);
    }

    @Transactional
    public void justificar(JustificacionForm form, Usuario justificadoPor) {
        AsistenciaJornada registro = asistenciaJornadaRepository.findById(form.getAsistenciaJornadaId())
                .orElseThrow(() -> NotFoundException.of("AsistenciaJornada", form.getAsistenciaJornadaId()));

        JustificacionInasistencia justificacion = justificacionInasistenciaRepository
                .findByAsistenciaJornada_Id(registro.getId()).orElseGet(JustificacionInasistencia::new);
        justificacion.setAsistenciaJornada(registro);
        justificacion.setTipo(form.getTipo());
        justificacion.setDescripcion(form.getDescripcion());
        justificacion.setJustificadoPor(justificadoPor);
        if (form.getDocumento() != null && !form.getDocumento().isEmpty()) {
            var guardado = almacenamientoService.guardar(form.getDocumento(), "asistencia/justificaciones", DOCUMENTO_MAX_MB);
            justificacion.setDocumentoPath(guardado.pathRelativo());
        }
        justificacionInasistenciaRepository.save(justificacion);

        registro.setTipo(TipoAsistenciaJornada.AUSENTE_JUSTIFICADO);
        asistenciaJornadaRepository.save(registro);

        recalcularAcumulado(registro.getAlumno().getId());
        auditService.registrar(MODULO, "JUSTIFICAR_INASISTENCIA", "AsistenciaJornada", registro.getId(), null,
                "tipo=" + form.getTipo());
    }

    /**
     * Recalcula el acumulado del alumno para el ciclo lectivo activo y evalua
     * los umbrales de la seccion 5.2.3, generando alertas y notificaciones
     * solo la primera vez que se cruza cada umbral (no en cada falta posterior).
     */
    @Transactional
    public void recalcularAcumulado(Long alumnoId) {
        CicloLectivo activo = cicloActivo();
        AlumnoCicloLectivo acl = alumnoCicloLectivoRepository.findByAlumno_IdAndCicloLectivo_Id(alumnoId, activo.getId())
                .orElse(null);
        if (acl == null) {
            return; // alumno sin trayectoria en el ciclo activo (ej. dado de baja)
        }

        BigDecimal sumaAntes = acl.getInasistenciasAcumuladas();
        List<AsistenciaJornada> registros = asistenciaJornadaRepository
                .findAllByAlumno_IdAndFechaBetweenOrderByFecha(alumnoId, activo.getFechaInicioC1(), activo.getFechaFinC2());

        BigDecimal sumaNueva = registros.stream()
                .map(r -> r.getTipo().getEquivalencia())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        acl.setInasistenciasAcumuladas(sumaNueva);

        ParametroAcademico param = parametroAcademicoService.obtener();
        Alumno alumno = acl.getAlumno();

        cruzaUmbral(sumaAntes, sumaNueva, param.getUmbralFalta1()) .ifPresent(u ->
                notificarUmbral(alumno, acl, "Umbral " + param.getUmbralFalta1() + " inasistencias", Alerta.Prioridad.MEDIA,
                        "%s acumuló %s inasistencias en el ciclo lectivo".formatted(alumno.getNombreCompleto(), sumaNueva)));

        cruzaUmbral(sumaAntes, sumaNueva, param.getUmbralFalta2()).ifPresent(u ->
                notificarUmbral(alumno, acl, "Umbral " + param.getUmbralFalta2() + " inasistencias", Alerta.Prioridad.ALTA,
                        "%s acumuló %s inasistencias. Se solicita reunión de seguimiento.".formatted(alumno.getNombreCompleto(), sumaNueva)));

        // Umbral 3 (seccion 5.2.3): determina el umbral EFECTIVO de condicion libre, una unica vez.
        if (acl.getUmbralCondicionLibreEfectivo() == null
                && sumaAntes.compareTo(BigDecimal.valueOf(param.getUmbralFalta3())) < 0
                && sumaNueva.compareTo(BigDecimal.valueOf(param.getUmbralFalta3())) >= 0) {
            BigDecimal justificadas = registros.stream()
                    .filter(r -> r.getTipo() == TipoAsistenciaJornada.AUSENTE_JUSTIFICADO)
                    .map(r -> r.getTipo().getEquivalencia())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            boolean mitadOMasJustificadas = justificadas.multiply(BigDecimal.valueOf(2)).compareTo(sumaNueva) >= 0;
            int umbralEfectivo = mitadOMasJustificadas ? param.getUmbralFalta4CondicionLibre() : param.getUmbralFalta3() + 1;
            acl.setUmbralCondicionLibreEfectivo(umbralEfectivo);

            notificarUmbral(alumno, acl, "Umbral critico " + param.getUmbralFalta3() + " inasistencias", Alerta.Prioridad.CRITICA,
                    "%s alcanzó %s inasistencias. %s".formatted(alumno.getNombreCompleto(), sumaNueva,
                            mitadOMasJustificadas
                                    ? "Se habilitan 8 inasistencias adicionales (Condicion Libre a las " + umbralEfectivo + ")."
                                    : "La proxima inasistencia activa la Condicion Libre."));
        }

        // Umbral 4: activacion de Condicion Libre segun el umbral efectivo ya determinado.
        if (!acl.isCondicionLibre() && acl.getUmbralCondicionLibreEfectivo() != null
                && sumaNueva.compareTo(BigDecimal.valueOf(acl.getUmbralCondicionLibreEfectivo())) >= 0) {
            activarCondicionLibre(alumno, acl);
        }

        alumnoCicloLectivoRepository.save(acl);
    }

    private java.util.Optional<Boolean> cruzaUmbral(BigDecimal antes, BigDecimal nueva, int umbral) {
        boolean cruzo = antes.compareTo(BigDecimal.valueOf(umbral)) < 0 && nueva.compareTo(BigDecimal.valueOf(umbral)) >= 0;
        return cruzo ? java.util.Optional.of(true) : java.util.Optional.empty();
    }

    private void activarCondicionLibre(Alumno alumno, AlumnoCicloLectivo acl) {
        acl.setCondicionLibre(true);
        acl.setCondicionLibreDesde(java.time.LocalDateTime.now());

        List<String> materiasARendir = materiaRepository.findAllByActivaTrueOrderByNombre().stream()
                .filter(m -> porcentajeAsistenciaMateria(alumno.getId(), m.getId())
                        .compareTo(parametroAcademicoService.obtener().getPorcentajeAsistenciaMateria()) < 0)
                .map(m -> m.getNombre())
                .toList();

        notificarUmbral(alumno, acl, "Condicion Libre activada", Alerta.Prioridad.CRITICA,
                "%s pasó a Condición Libre. Materias a rendir: %s".formatted(
                        alumno.getNombreCompleto(), materiasARendir.isEmpty() ? "(ninguna con <75% aun registrada)" : String.join(", ", materiasARendir)));
    }

    private void notificarUmbral(Alumno alumno, AlumnoCicloLectivo acl, String tipo, Alerta.Prioridad prioridad, String descripcion) {
        alertaService.crear(tipo, prioridad, alumno, descripcion);
        notificacionService.notificarTutores(alumno, "UMBRAL_INASISTENCIAS", tipo, descripcion);

        List<AlumnoTutor> tutores = alumnoTutorRepository.findAllByAlumno_Id(alumno.getId());
        for (AlumnoTutor vinculo : tutores) {
            if (vinculo.isRecibeNotificaciones()) {
                emailService.enviarHtml(vinculo.getTutor().getEmail(), tipo,
                        "<p>" + descripcion + "</p>");
            }
        }
    }

    private CicloLectivo cicloActivo() {
        return cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));
    }
}
