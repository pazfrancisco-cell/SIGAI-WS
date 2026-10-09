package ar.edu.wellspring.sigaiws.modules.reporte.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.notification.EmailService;
import ar.edu.wellspring.sigaiws.common.pdf.PdfTableGenerator;
import ar.edu.wellspring.sigaiws.modules.alerta.domain.Alerta;
import ar.edu.wellspring.sigaiws.modules.alerta.service.AlertaService;
import ar.edu.wellspring.sigaiws.modules.mensajeria.service.NotificacionService;
import ar.edu.wellspring.sigaiws.common.storage.AlmacenamientoService;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoCicloLectivo;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoTutor;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoCicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoTutorRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.service.AlumnoService;
import ar.edu.wellspring.sigaiws.modules.calificacion.service.CalificacionService;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.Materia;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.MateriaRepository;
import ar.edu.wellspring.sigaiws.modules.disciplina.service.DisciplinaService;
import ar.edu.wellspring.sigaiws.modules.edfisica.service.AptoFisicoService;
import ar.edu.wellspring.sigaiws.modules.edfisica.service.EducacionFisicaService;
import ar.edu.wellspring.sigaiws.modules.reporte.domain.ReporteGenerado;
import ar.edu.wellspring.sigaiws.modules.reporte.repository.ReporteGeneradoRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Modulo 5.8 (Reportes Institucionales). */
@Service
@RequiredArgsConstructor
public class ReporteService {

    private final AlumnoService alumnoService;
    private final CalificacionService calificacionService;
    private final MateriaRepository materiaRepository;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final AlumnoCicloLectivoRepository alumnoCicloLectivoRepository;
    private final AlumnoTutorRepository alumnoTutorRepository;
    private final DisciplinaService disciplinaService;
    private final EducacionFisicaService educacionFisicaService;
    private final AptoFisicoService aptoFisicoService;
    private final ReporteGeneradoRepository reporteGeneradoRepository;
    private final BoletinPdfGenerator boletinPdfGenerator;
    private final FipPdfGenerator fipPdfGenerator;
    private final PdfTableGenerator pdfTableGenerator;
    private final AlmacenamientoService almacenamientoService;
    private final AuditService auditService;
    private final AlertaService alertaService;
    private final NotificacionService notificacionService;
    private final EmailService emailService;

    private static final String TIPO_ALERTA_BOLETIN = "Boletin publicado";

    /** Boletin individual (desde el legajo): avisa a la familia y deja una alerta informativa para gestion. */
    @Transactional
    public ReporteGenerado generarBoletin(Long alumnoId, int cuatrimestre, Usuario generadoPor) {
        ReporteGenerado reporte = publicarBoletin(alumnoId, cuatrimestre, generadoPor);
        Alumno alumno = reporte.getAlumno();
        alertaService.crear(TIPO_ALERTA_BOLETIN, Alerta.Prioridad.INFO, alumno,
                "Se publico el boletin del cuatrimestre %d de %s.".formatted(cuatrimestre, alumno.getNombreCompleto()),
                "REPORTE", reporte.getId());
        return reporte;
    }

    /** Genera, archiva y avisa a la familia (portal + email). La alerta para gestion la decide quien llama. */
    private ReporteGenerado publicarBoletin(Long alumnoId, int cuatrimestre, Usuario generadoPor) {
        Alumno alumno = alumnoService.obtener(alumnoId);
        byte[] pdf = construirBoletinPdf(alumno, cuatrimestre);
        var guardado = almacenamientoService.guardarBytes(pdf, "reportes/boletines",
                "Boletin_" + alumno.getApellido() + "_" + alumno.getNombre() + "_C" + cuatrimestre + ".pdf", ".pdf");

        ReporteGenerado reporte = new ReporteGenerado();
        reporte.setTipo(ReporteGenerado.Tipo.BOLETIN);
        reporte.setAlumno(alumno);
        reporte.setCuatrimestre(cuatrimestre);
        reporte.setPathArchivo(guardado.pathRelativo());
        reporte.setGeneradoPor(generadoPor);
        reporte = reporteGeneradoRepository.save(reporte);

        auditService.registrar("REPORTES", "GENERAR_BOLETIN", "ReporteGenerado", reporte.getId(), null,
                "alumno=" + alumno.getNombreCompleto() + ", cuatrimestre=" + cuatrimestre);
        avisarFamilia(alumno, cuatrimestre);
        return reporte;
    }

    /** Seccion 5.13.2 "Boletin publicado": notificacion en el portal y email (plantilla BOLETIN_PUBLICADO) a los tutores. */
    private void avisarFamilia(Alumno alumno, int cuatrimestre) {
        notificacionService.notificarTutores(alumno, "BOLETIN_PUBLICADO", "Boletin publicado: " + alumno.getNombreCompleto(),
                "Se publico el boletin del cuatrimestre %d de %s. Podes verlo en Reportes Disponibles."
                        .formatted(cuatrimestre, alumno.getNombreCompleto()));
        for (AlumnoTutor vinculo : alumnoTutorRepository.findAllByAlumno_Id(alumno.getId())) {
            if (vinculo.isRecibeNotificaciones()) {
                emailService.enviarConPlantilla(vinculo.getTutor().getEmail(), "BOLETIN_PUBLICADO", java.util.Map.of(
                        "nombre_alumno", alumno.getNombreCompleto(),
                        "cuatrimestre", String.valueOf(cuatrimestre)));
            }
        }
    }

    private byte[] construirBoletinPdf(Alumno alumno, int cuatrimestre) {
        CicloLectivo ciclo = cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));
        var cicloActualAlumno = alumnoService.cicloActualDe(alumno.getId());
        Long cursoDivisionId = cicloActualAlumno.getCursoDivision().getId();

        List<Materia> materias = materiaRepository.findAllByActivaTrueOrderByNombre().stream()
                .filter(m -> m.getCursosDivisiones().stream().anyMatch(cd -> cd.getId().equals(cursoDivisionId)))
                .toList();

        List<BoletinPdfGenerator.FilaMateria> filas = new ArrayList<>();
        for (Materia materia : materias) {
            List<BoletinPdfGenerator.FilaEvaluacion> evaluaciones = calificacionService
                    .notasDe(alumno.getId(), materia.getId(), cuatrimestre).stream()
                    .map(nota -> new BoletinPdfGenerator.FilaEvaluacion(
                            etiquetaTipo(nota.getEvaluacion().getTipo()),
                            nota.getEvaluacion().getFecha().toString(),
                            nota.getEvaluacion().getTitulo(),
                            nota.getValor().toString()))
                    .toList();
            BigDecimal promedio = calificacionService.promedioCuatrimestral(alumno.getId(), materia.getId(), cuatrimestre);
            boolean aprobada = promedio != null && calificacionService.estaAprobada(promedio);
            filas.add(new BoletinPdfGenerator.FilaMateria(
                    materia.getNombre(),
                    evaluaciones,
                    promedio != null ? promedio.toString() : "—",
                    promedio == null ? "Sin datos" : (aprobada ? "Aprobada" : "A recuperar")));
        }
        return boletinPdfGenerator.generar(alumno, ciclo, cuatrimestre, filas);
    }

    /**
     * Generacion masiva de boletines (seccion 5.8.4): un boletin por cada alumno activo del
     * curso/division, entregados juntos en un .zip. Cada boletin tambien queda archivado en el
     * legajo del alumno correspondiente, igual que si se hubiera generado individualmente.
     */
    @Transactional
    public byte[] generarBoletinesCursoZip(Long cursoDivisionId, int cuatrimestre, Usuario generadoPor) {
        List<Alumno> alumnos = alumnoService.alumnosDeDivisionEnCicloActivo(cursoDivisionId);
        if (alumnos.isEmpty()) {
            throw new BusinessException("No hay alumnos activos en ese curso/division.");
        }
        try {
            var buffer = new java.io.ByteArrayOutputStream();
            try (var zip = new java.util.zip.ZipOutputStream(buffer, java.nio.charset.StandardCharsets.UTF_8)) {
                for (Alumno alumno : alumnos) {
                    publicarBoletin(alumno.getId(), cuatrimestre, generadoPor);
                    byte[] pdf = construirBoletinPdf(alumno, cuatrimestre);
                    zip.putNextEntry(new java.util.zip.ZipEntry(
                            alumno.getApellido() + "_" + alumno.getNombre() + "_C" + cuatrimestre + ".pdf"));
                    zip.write(pdf);
                    zip.closeEntry();
                }
            }
            // Una sola alerta para todo el curso (no una por alumno), para no llenar el Centro de Alertas.
            String curso = alumnoService.cicloActualDe(alumnos.get(0).getId()).getCursoDivision().getNombreCompleto();
            alertaService.crear(TIPO_ALERTA_BOLETIN, Alerta.Prioridad.INFO, null,
                    "Se publicaron %d boletines de %s (cuatrimestre %d).".formatted(alumnos.size(), curso, cuatrimestre),
                    "CURSO_DIVISION", cursoDivisionId);
            auditService.registrar("REPORTES", "GENERAR_BOLETINES_MASIVO", "CursoDivision", cursoDivisionId, null,
                    "cuatrimestre=" + cuatrimestre + ", alumnos=" + alumnos.size());
            return buffer.toByteArray();
        } catch (java.io.IOException e) {
            throw new BusinessException("No se pudo generar el archivo ZIP: " + e.getMessage());
        }
    }

    /** FIP (Ficha de Informacion Pedagogica), seccion 5.8 (extension). */
    @Transactional
    public ReporteGenerado generarFip(Long alumnoId, String observacionesPedagogicas, Usuario generadoPor) {
        Alumno alumno = alumnoService.obtener(alumnoId);
        CicloLectivo ciclo = cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));
        var cicloActualAlumno = alumnoService.cicloActualDe(alumnoId);
        Long cursoDivisionId = cicloActualAlumno.getCursoDivision().getId();

        List<FipPdfGenerator.Tutor> tutores = alumnoTutorRepository.findAllByAlumno_Id(alumnoId).stream()
                .map(v -> new FipPdfGenerator.Tutor(v.getTutor().getNombreCompleto(), v.getVinculo().name(),
                        v.getTutor().getTelefono(), v.getTutor().getEmail()))
                .toList();

        List<Materia> materias = materiaRepository.findAllByActivaTrueOrderByNombre().stream()
                .filter(m -> m.getCursosDivisiones().stream().anyMatch(cd -> cd.getId().equals(cursoDivisionId)))
                .toList();
        List<FipPdfGenerator.MateriaDificultad> materiasConDificultad = new ArrayList<>();
        for (Materia materia : materias) {
            BigDecimal promedio = calificacionService.promedioAnual(alumnoId, materia.getId());
            if (promedio != null && !calificacionService.estaAprobada(promedio)) {
                materiasConDificultad.add(new FipPdfGenerator.MateriaDificultad(materia.getNombre(), promedio.toString()));
            }
        }

        List<FipPdfGenerator.Observacion> observaciones = disciplinaService.historialDe(alumnoId).stream()
                .map(o -> new FipPdfGenerator.Observacion(o.getFecha().toString(),
                        o.getCategoria() != null ? o.getCategoria() : "-", o.getDescripcion()))
                .toList();

        List<FipPdfGenerator.Restriccion> restricciones = educacionFisicaService.restriccionesDe(alumnoId).stream()
                .map(r -> new FipPdfGenerator.Restriccion(r.getDescripcion(),
                        r.isActiva() ? "Vigente" : "Finalizada el " + r.getFechaFin()))
                .toList();

        String estadoAptoFisico = aptoFisicoService.obtenerDelCicloActivo(alumnoId)
                .map(a -> "Presentado el " + a.getFechaPresentacion())
                .orElse(aptoFisicoService.esSaf(alumnoId) ? "No presentado (SAF)" : "No presentado");

        byte[] pdf = fipPdfGenerator.generar(alumno, ciclo, cicloActualAlumno.getCursoDivision().getNombreCompleto(),
                tutores, materiasConDificultad, cicloActualAlumno.isCondicionLibre(), observaciones, restricciones,
                estadoAptoFisico, observacionesPedagogicas);
        var guardado = almacenamientoService.guardarBytes(pdf, "reportes/fip",
                "FIP_" + alumno.getApellido() + "_" + alumno.getNombre() + ".pdf", ".pdf");

        ReporteGenerado reporte = new ReporteGenerado();
        reporte.setTipo(ReporteGenerado.Tipo.FIP);
        reporte.setAlumno(alumno);
        reporte.setPathArchivo(guardado.pathRelativo());
        reporte.setObservaciones(observacionesPedagogicas);
        reporte.setGeneradoPor(generadoPor);
        reporte = reporteGeneradoRepository.save(reporte);

        auditService.registrar("REPORTES", "GENERAR_FIP", "ReporteGenerado", reporte.getId(), null,
                "alumno=" + alumno.getNombreCompleto());
        return reporte;
    }

    /** RITE (Registro de Informacion de Trayectoria Escolar), seccion 5.8 (extension): historial completo multi-año. */
    @Transactional
    public ReporteGenerado generarRite(Long alumnoId, Usuario generadoPor) {
        Alumno alumno = alumnoService.obtener(alumnoId);
        List<AlumnoCicloLectivo> trayectoria = alumnoCicloLectivoRepository.findAllByAlumno_IdOrderByCicloLectivo_AnioDesc(alumnoId);
        if (trayectoria.isEmpty()) {
            throw new BusinessException("El alumno no tiene trayectoria registrada en ningun ciclo lectivo.");
        }

        List<PdfTableGenerator.Seccion> secciones = new ArrayList<>();
        for (AlumnoCicloLectivo acl : trayectoria) {
            Long cicloId = acl.getCicloLectivo().getId();
            Long cursoDivisionId = acl.getCursoDivision().getId();
            List<Materia> materias = materiaRepository.findAllByActivaTrueOrderByNombre().stream()
                    .filter(m -> m.getCursosDivisiones().stream().anyMatch(cd -> cd.getId().equals(cursoDivisionId)))
                    .toList();

            List<List<String>> filas = new ArrayList<>();
            for (Materia materia : materias) {
                BigDecimal c1 = calificacionService.promedioCuatrimestral(alumnoId, materia.getId(), 1, cicloId);
                BigDecimal c2 = calificacionService.promedioCuatrimestral(alumnoId, materia.getId(), 2, cicloId);
                BigDecimal anual = calificacionService.promedioAnual(alumnoId, materia.getId(), cicloId);
                filas.add(List.of(materia.getNombre(),
                        c1 != null ? c1.toString() : "—",
                        c2 != null ? c2.toString() : "—",
                        anual != null ? anual.toString() : "—"));
            }

            String condicion = acl.isCondicionLibre() ? "Condicion Libre" : (acl.isRepetidor() ? "Repitiendo" : "Regular");
            String subtitulo = "Ciclo " + acl.getCicloLectivo().getAnio() + " — " + acl.getCursoDivision().getNombreCompleto()
                    + " — " + condicion + " — Inasistencias acumuladas: " + acl.getInasistenciasAcumuladas();
            secciones.add(new PdfTableGenerator.Seccion(subtitulo,
                    List.of("Materia", "Promedio C1", "Promedio C2", "Promedio Anual"), filas));
        }

        List<String> encabezado = List.of("Alumno: " + alumno.getNombreCompleto(), "DNI: " + alumno.getDni());
        byte[] pdf = pdfTableGenerator.generarAgrupado("Registro de Información de Trayectoria Escolar (RITE)", encabezado, secciones);
        var guardado = almacenamientoService.guardarBytes(pdf, "reportes/rite",
                "RITE_" + alumno.getApellido() + "_" + alumno.getNombre() + ".pdf", ".pdf");

        ReporteGenerado reporte = new ReporteGenerado();
        reporte.setTipo(ReporteGenerado.Tipo.RITE);
        reporte.setAlumno(alumno);
        reporte.setPathArchivo(guardado.pathRelativo());
        reporte.setGeneradoPor(generadoPor);
        reporte = reporteGeneradoRepository.save(reporte);

        auditService.registrar("REPORTES", "GENERAR_RITE", "ReporteGenerado", reporte.getId(), null,
                "alumno=" + alumno.getNombreCompleto());
        return reporte;
    }

    @Transactional(readOnly = true)
    public List<ReporteGenerado> reportesDe(Long alumnoId) {
        return reporteGeneradoRepository.findAllByAlumno_IdOrderByGeneradoEnDesc(alumnoId);
    }

    static String etiquetaTipo(ar.edu.wellspring.sigaiws.modules.calificacion.domain.TipoEvaluacion tipo) {
        return switch (tipo) {
            case PRUEBA_ESCRITA -> "Prueba Escrita";
            case EVALUACION_ORAL -> "Evaluacion Oral";
            case TRABAJO_PRACTICO -> "Trabajo Practico";
            case EVALUACION_CONCEPTUAL -> "Evaluacion Conceptual";
        };
    }
}
