package ar.edu.wellspring.sigaiws.modules.calificacion.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.common.notification.EmailService;
import ar.edu.wellspring.sigaiws.modules.mensajeria.service.NotificacionService;
import ar.edu.wellspring.sigaiws.modules.alerta.domain.Alerta;
import ar.edu.wellspring.sigaiws.modules.alerta.service.AlertaService;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoTutor;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoTutorRepository;
import ar.edu.wellspring.sigaiws.modules.calificacion.domain.Evaluacion;
import ar.edu.wellspring.sigaiws.modules.calificacion.domain.Nota;
import ar.edu.wellspring.sigaiws.modules.calificacion.domain.SolicitudCorreccionNota;
import ar.edu.wellspring.sigaiws.modules.calificacion.dto.CargaNotasForm;
import ar.edu.wellspring.sigaiws.modules.calificacion.dto.EvaluacionForm;
import ar.edu.wellspring.sigaiws.modules.calificacion.dto.SolicitudCorreccionForm;
import ar.edu.wellspring.sigaiws.modules.calificacion.repository.EvaluacionRepository;
import ar.edu.wellspring.sigaiws.modules.calificacion.repository.NotaRepository;
import ar.edu.wellspring.sigaiws.modules.calificacion.repository.SolicitudCorreccionNotaRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.ParametroAcademico;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.MateriaRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.service.ParametroAcademicoService;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Modulo 5.3 (Calificaciones). */
@Service
@RequiredArgsConstructor
public class CalificacionService {

    private static final String MODULO = "CALIFICACIONES";
    private static final String TIPO_BAJO_RENDIMIENTO = "Bajo rendimiento academico";
    private static final String TIPO_SOLICITUD_CORRECCION = "Solicitud de correccion pendiente";
    private static final String ENTIDAD_SOLICITUD = "SOLICITUD_CORRECCION";

    private final EvaluacionRepository evaluacionRepository;
    private final NotaRepository notaRepository;
    private final SolicitudCorreccionNotaRepository solicitudRepository;
    private final AlumnoRepository alumnoRepository;
    private final AlumnoTutorRepository alumnoTutorRepository;
    private final MateriaRepository materiaRepository;
    private final CursoDivisionRepository cursoDivisionRepository;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final ParametroAcademicoService parametroAcademicoService;
    private final AlertaService alertaService;
    private final EmailService emailService;
    private final NotificacionService notificacionService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<Evaluacion> evaluacionesDe(Long materiaId, Long cursoDivisionId, int cuatrimestre) {
        CicloLectivo activo = cicloActivo();
        return evaluacionRepository.findAllByMateria_IdAndCursoDivision_IdAndCicloLectivo_IdAndCuatrimestreOrderByFecha(
                materiaId, cursoDivisionId, activo.getId(), cuatrimestre);
    }

    @Transactional
    public Evaluacion crearEvaluacion(EvaluacionForm form, Usuario creadoPor) {
        if (form.getFecha().isAfter(java.time.LocalDate.now())) {
            throw new BusinessException("La fecha de la evaluacion no puede ser futura.");
        }
        CicloLectivo activo = cicloActivo();
        verificarCuatrimestreAbierto(activo, form.getCuatrimestre());

        Evaluacion evaluacion = new Evaluacion();
        evaluacion.setMateria(materiaRepository.findById(form.getMateriaId())
                .orElseThrow(() -> NotFoundException.of("Materia", form.getMateriaId())));
        evaluacion.setCursoDivision(cursoDivisionRepository.findById(form.getCursoDivisionId())
                .orElseThrow(() -> NotFoundException.of("CursoDivision", form.getCursoDivisionId())));
        evaluacion.setCicloLectivo(activo);
        evaluacion.setCuatrimestre(form.getCuatrimestre());
        evaluacion.setTipo(form.getTipo());
        evaluacion.setTitulo(form.getTitulo());
        evaluacion.setFecha(form.getFecha());
        evaluacion.setCreadoPor(creadoPor);
        evaluacion = evaluacionRepository.save(evaluacion);
        auditService.registrar(MODULO, "CREAR_EVALUACION", "Evaluacion", evaluacion.getId(), null, form.getTitulo());
        return evaluacion;
    }

    /** Correccion de una evaluacion mal cargada (seccion 5.3.4): mismas reglas de cierre que crearla. */
    @Transactional
    public Evaluacion editarEvaluacion(Long evaluacionId, EvaluacionForm form, Usuario editadoPor) {
        Evaluacion evaluacion = evaluacionRepository.findById(evaluacionId)
                .orElseThrow(() -> NotFoundException.of("Evaluacion", evaluacionId));
        verificarCuatrimestreAbierto(evaluacion.getCicloLectivo(), evaluacion.getCuatrimestre());
        if (form.getFecha().isAfter(java.time.LocalDate.now())) {
            throw new BusinessException("La fecha de la evaluacion no puede ser futura.");
        }

        evaluacion.setTipo(form.getTipo());
        evaluacion.setTitulo(form.getTitulo());
        evaluacion.setFecha(form.getFecha());
        evaluacion = evaluacionRepository.save(evaluacion);
        auditService.registrar(MODULO, "EDITAR_EVALUACION", "Evaluacion", evaluacion.getId(), null, form.getTitulo());
        return evaluacion;
    }

    /** Elimina una evaluacion creada por error, junto con las notas y solicitudes de correccion asociadas. */
    @Transactional
    public void eliminarEvaluacion(Long evaluacionId, Usuario eliminadoPor) {
        Evaluacion evaluacion = evaluacionRepository.findById(evaluacionId)
                .orElseThrow(() -> NotFoundException.of("Evaluacion", evaluacionId));
        verificarCuatrimestreAbierto(evaluacion.getCicloLectivo(), evaluacion.getCuatrimestre());

        List<Nota> notas = notaRepository.findAllByEvaluacion_Id(evaluacionId);
        if (!notas.isEmpty()) {
            solicitudRepository.deleteAllByNota_IdIn(notas.stream().map(Nota::getId).toList());
            notaRepository.deleteAll(notas);
        }
        evaluacionRepository.delete(evaluacion);
        auditService.registrar(MODULO, "ELIMINAR_EVALUACION", "Evaluacion", evaluacionId, evaluacion.getTitulo(), null);
    }

    @Transactional
    public void cargarNotas(CargaNotasForm form, Usuario cargadoPor) {
        Evaluacion evaluacion = evaluacionRepository.findById(form.getEvaluacionId())
                .orElseThrow(() -> NotFoundException.of("Evaluacion", form.getEvaluacionId()));
        verificarCuatrimestreAbierto(evaluacion.getCicloLectivo(), evaluacion.getCuatrimestre());

        for (CargaNotasForm.Item item : form.getItems()) {
            if (item.getValor() == null || item.getValor().isBlank()) {
                continue;
            }
            BigDecimal valor;
            try {
                valor = new BigDecimal(item.getValor().replace(",", "."));
            } catch (NumberFormatException e) {
                continue;
            }
            if (valor.compareTo(BigDecimal.ONE) < 0 || valor.compareTo(BigDecimal.TEN) > 0) {
                continue;
            }

            Nota nota = notaRepository.findByEvaluacion_IdAndAlumno_Id(evaluacion.getId(), item.getAlumnoId())
                    .orElseGet(Nota::new);
            Alumno alumno = alumnoRepository.findById(item.getAlumnoId())
                    .orElseThrow(() -> NotFoundException.of("Alumno", item.getAlumnoId()));
            nota.setEvaluacion(evaluacion);
            nota.setAlumno(alumno);
            nota.setValor(valor);
            nota.setCargadoPor(cargadoPor);
            notaRepository.save(nota);

            verificarBajoRendimiento(alumno, evaluacion);
        }
        auditService.registrar(MODULO, "CARGAR_NOTAS", "Evaluacion", evaluacion.getId(), null,
                "alumnos=" + form.getItems().size());
    }

    /** Seccion 5.3.2: promedio simple de las evaluaciones del cuatrimestre, en el ciclo lectivo activo. */
    @Transactional(readOnly = true)
    public BigDecimal promedioCuatrimestral(Long alumnoId, Long materiaId, int cuatrimestre) {
        return promedioCuatrimestral(alumnoId, materiaId, cuatrimestre, cicloActivo().getId());
    }

    /** Igual que {@link #promedioCuatrimestral(Long, Long, int)} pero para un ciclo lectivo especifico (seccion 5.8: RITE multi-año). */
    @Transactional(readOnly = true)
    public BigDecimal promedioCuatrimestral(Long alumnoId, Long materiaId, int cuatrimestre, Long cicloLectivoId) {
        List<BigDecimal> delCuatrimestre = notasDe(alumnoId, materiaId, cuatrimestre, cicloLectivoId).stream().map(Nota::getValor).toList();
        if (delCuatrimestre.isEmpty()) {
            return null;
        }
        BigDecimal suma = delCuatrimestre.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(BigDecimal.valueOf(delCuatrimestre.size()), 2, RoundingMode.HALF_UP);
    }

    /** Todas las notas del alumno en una materia y cuatrimestre del ciclo activo, una por evaluacion (seccion 5.8.1: boletin detallado). */
    @Transactional(readOnly = true)
    public List<Nota> notasDe(Long alumnoId, Long materiaId, int cuatrimestre) {
        return notasDe(alumnoId, materiaId, cuatrimestre, cicloActivo().getId());
    }

    /** Igual que {@link #notasDe(Long, Long, int)} pero para un ciclo lectivo especifico. */
    @Transactional(readOnly = true)
    public List<Nota> notasDe(Long alumnoId, Long materiaId, int cuatrimestre, Long cicloLectivoId) {
        return notaRepository.findAllByAlumno_IdAndEvaluacion_Materia_IdAndEvaluacion_CicloLectivo_IdOrderByEvaluacion_Fecha(
                        alumnoId, materiaId, cicloLectivoId)
                .stream()
                .filter(n -> n.getEvaluacion().getCuatrimestre() == cuatrimestre)
                .toList();
    }

    /** Seccion 5.3.2: promedio anual = promedio de los dos promedios cuatrimestrales; aprueba si >= nota minima. */
    @Transactional(readOnly = true)
    public BigDecimal promedioAnual(Long alumnoId, Long materiaId) {
        return promedioAnual(alumnoId, materiaId, cicloActivo().getId());
    }

    /** Igual que {@link #promedioAnual(Long, Long)} pero para un ciclo lectivo especifico (seccion 5.8: RITE multi-año). */
    @Transactional(readOnly = true)
    public BigDecimal promedioAnual(Long alumnoId, Long materiaId, Long cicloLectivoId) {
        BigDecimal c1 = promedioCuatrimestral(alumnoId, materiaId, 1, cicloLectivoId);
        BigDecimal c2 = promedioCuatrimestral(alumnoId, materiaId, 2, cicloLectivoId);
        if (c1 == null && c2 == null) {
            return null;
        }
        if (c1 == null) {
            return c2;
        }
        if (c2 == null) {
            return c1;
        }
        return c1.add(c2).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public boolean estaAprobada(BigDecimal promedioAnual) {
        if (promedioAnual == null) {
            return false;
        }
        return promedioAnual.compareTo(parametroAcademicoService.obtener().getNotaMinimaAprobacion()) >= 0;
    }

    @Transactional
    public void solicitarCorreccion(SolicitudCorreccionForm form, Usuario solicitadoPor) {
        Nota nota = notaRepository.findById(form.getNotaId())
                .orElseThrow(() -> NotFoundException.of("Nota", form.getNotaId()));

        SolicitudCorreccionNota solicitud = new SolicitudCorreccionNota();
        solicitud.setNota(nota);
        solicitud.setNotaAnterior(nota.getValor());
        solicitud.setNotaPropuesta(form.getNotaPropuesta());
        solicitud.setJustificacion(form.getJustificacion());
        solicitud.setSolicitadoPor(solicitadoPor);
        solicitud = solicitudRepository.save(solicitud);

        // Seccion 5.13.2: queda en el Centro de Alertas hasta que gestion la apruebe o rechace.
        Evaluacion evaluacion = nota.getEvaluacion();
        alertaService.crear(TIPO_SOLICITUD_CORRECCION, Alerta.Prioridad.MEDIA, nota.getAlumno(),
                "%s pidio corregir la nota de %s en %s (%s): %s -> %s. Motivo: %s".formatted(
                        solicitadoPor.getNombreCompleto(), nota.getAlumno().getNombreCompleto(),
                        evaluacion.getMateria().getNombre(), evaluacion.getTitulo(),
                        nota.getValor(), form.getNotaPropuesta(), form.getJustificacion()),
                ENTIDAD_SOLICITUD, solicitud.getId());

        auditService.registrar(MODULO, "SOLICITAR_CORRECCION_NOTA", "SolicitudCorreccionNota", solicitud.getId(),
                nota.getValor(), form.getNotaPropuesta());
    }

    @Transactional(readOnly = true)
    public List<SolicitudCorreccionNota> solicitudesPendientes() {
        return solicitudRepository.findAllByEstadoOrderBySolicitadoEnDesc(SolicitudCorreccionNota.Estado.PENDIENTE);
    }

    @Transactional
    public void resolverSolicitud(Long id, boolean aprobar, String motivoRechazo, Usuario resolutor) {
        SolicitudCorreccionNota solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("SolicitudCorreccionNota", id));

        solicitud.setResueltoPor(resolutor);
        solicitud.setResueltoEn(LocalDateTime.now());
        if (aprobar) {
            solicitud.setEstado(SolicitudCorreccionNota.Estado.APROBADA);
            Nota nota = solicitud.getNota();
            nota.setValor(solicitud.getNotaPropuesta());
            notaRepository.save(nota);
        } else {
            solicitud.setEstado(SolicitudCorreccionNota.Estado.RECHAZADA);
            solicitud.setMotivoRechazo(motivoRechazo);
        }
        solicitudRepository.save(solicitud);

        String resultado = aprobar ? "aprobada" : "rechazada";
        alertaService.resolverPorEntidad(TIPO_SOLICITUD_CORRECCION, ENTIDAD_SOLICITUD, solicitud.getId(),
                "Solicitud " + resultado + " por " + resolutor.getNombreCompleto(), resolutor);

        Nota nota = solicitud.getNota();
        Evaluacion evaluacion = nota.getEvaluacion();
        String detalle = "La correccion de la nota de %s en %s (%s) fue %s.".formatted(
                nota.getAlumno().getNombreCompleto(), evaluacion.getMateria().getNombre(), evaluacion.getTitulo(), resultado)
                + (aprobar ? " Nueva nota: " + solicitud.getNotaPropuesta() + "."
                           : (motivoRechazo != null && !motivoRechazo.isBlank() ? " Motivo: " + motivoRechazo : ""));
        notificacionService.notificar(solicitud.getSolicitadoPor().getId(), "SOLICITUD_CORRECCION_RESUELTA",
                "Tu solicitud de correccion fue " + resultado, detalle, ENTIDAD_SOLICITUD, solicitud.getId(),
                "/calificaciones/cargar?materiaId=" + evaluacion.getMateria().getId()
                        + "&cursoDivisionId=" + evaluacion.getCursoDivision().getId()
                        + "&cuatrimestre=" + evaluacion.getCuatrimestre());

        auditService.registrar(MODULO, aprobar ? "APROBAR_CORRECCION_NOTA" : "RECHAZAR_CORRECCION_NOTA",
                "SolicitudCorreccionNota", solicitud.getId(), null, null);
    }

    /** Seccion 5.3.7: alerta si el promedio parcial cae por debajo del umbral configurado. */
    private void verificarBajoRendimiento(Alumno alumno, Evaluacion evaluacion) {
        BigDecimal promedio = promedioCuatrimestral(alumno.getId(), evaluacion.getMateria().getId(), evaluacion.getCuatrimestre());
        if (promedio == null) {
            return;
        }
        ParametroAcademico param = parametroAcademicoService.obtener();
        if (promedio.compareTo(param.getUmbralBajoRendimiento()) < 0) {
            String descripcion = "%s tiene un promedio de %s en %s (por debajo de %s)".formatted(
                    alumno.getNombreCompleto(), promedio, evaluacion.getMateria().getNombre(), param.getUmbralBajoRendimiento());
            // Una sola alerta activa por alumno y materia: las notas bajas siguientes actualizan el promedio.
            boolean alertaNueva = alertaService.crearOActualizar(TIPO_BAJO_RENDIMIENTO, Alerta.Prioridad.MEDIA, alumno,
                    descripcion, "MATERIA", evaluacion.getMateria().getId());
            if (!alertaNueva) {
                return; // la familia ya fue avisada mientras la alerta siga abierta
            }
            notificacionService.notificarTutores(alumno, "BAJO_RENDIMIENTO", "Alerta de rendimiento academico", descripcion);

            for (AlumnoTutor vinculo : alumnoTutorRepository.findAllByAlumno_Id(alumno.getId())) {
                if (vinculo.isRecibeNotificaciones()) {
                    emailService.enviarHtml(vinculo.getTutor().getEmail(), "Alerta de rendimiento academico",
                            "<p>" + descripcion + "</p>");
                }
            }
        }
    }

    private CicloLectivo cicloActivo() {
        return cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));
    }

    /**
     * Seccion 6.4 (Cierre de Cuatrimestre): "los docentes pierden la capacidad
     * de agregar o editar notas sin solicitud de correccion aprobada".
     */
    private void verificarCuatrimestreAbierto(CicloLectivo ciclo, int cuatrimestre) {
        boolean cerrado = cuatrimestre == 1 ? ciclo.isCuatrimestre1Cerrado() : ciclo.isCuatrimestre2Cerrado();
        if (cerrado) {
            throw new BusinessException("El cuatrimestre " + cuatrimestre + " esta cerrado. "
                    + "Para modificar una nota ya cargada, use 'Solicitar correccion'.");
        }
    }
}
