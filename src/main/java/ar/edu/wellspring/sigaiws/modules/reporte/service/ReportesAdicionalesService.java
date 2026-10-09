package ar.edu.wellspring.sigaiws.modules.reporte.service;

import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.pdf.ExcelTableGenerator;
import ar.edu.wellspring.sigaiws.common.pdf.PdfTableGenerator;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoCicloLectivo;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoCicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.service.AlumnoService;
import ar.edu.wellspring.sigaiws.modules.asistencia.domain.AsistenciaJornada;
import ar.edu.wellspring.sigaiws.modules.asistencia.repository.AsistenciaJornadaRepository;
import ar.edu.wellspring.sigaiws.modules.asistencia.repository.JustificacionInasistenciaRepository;
import ar.edu.wellspring.sigaiws.modules.calificacion.service.CalificacionService;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CursoDivision;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.Materia;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.MateriaRepository;
import ar.edu.wellspring.sigaiws.modules.disciplina.service.DisciplinaService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seccion 5.8.3 (Reportes Adicionales): reportes de solo lectura generados
 * directamente desde los datos ya registrados en el sistema, sin persistir
 * un registro en ReporteGenerado (a diferencia del Boletin, que si queda
 * archivado en el legajo del alumno).
 */
@Service
@RequiredArgsConstructor
public class ReportesAdicionalesService {

    private final AlumnoService alumnoService;
    private final AlumnoCicloLectivoRepository alumnoCicloLectivoRepository;
    private final AsistenciaJornadaRepository asistenciaJornadaRepository;
    private final JustificacionInasistenciaRepository justificacionInasistenciaRepository;
    private final CalificacionService calificacionService;
    private final DisciplinaService disciplinaService;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final CursoDivisionRepository cursoDivisionRepository;
    private final MateriaRepository materiaRepository;
    private final PdfTableGenerator pdfTableGenerator;
    private final ExcelTableGenerator excelTableGenerator;

    /** Datos ya armados de un reporte tabular, reutilizados para generarlo tanto en PDF como en Excel (seccion 5.8.3). */
    private record TablaReporte(String titulo, List<String> encabezado, List<String> columnas, List<List<String>> filas) {}

    /** Reporte de Asistencia individual (seccion 5.8.1), organizado por mes para facilitar su lectura. */
    @Transactional(readOnly = true)
    public byte[] reporteAsistenciaAlumno(Long alumnoId) {
        Alumno alumno = alumnoService.obtener(alumnoId);
        List<AsistenciaJornada> registros = asistenciaJornadaRepository.findAllByAlumno_IdOrderByFechaDesc(alumnoId);

        List<String> columnas = List.of("Fecha", "Turno", "Tipo", "Justificada");
        Map<YearMonth, List<List<String>>> porMes = new LinkedHashMap<>();
        BigDecimal totalEquivalente = BigDecimal.ZERO;
        int totalJustificadas = 0;
        for (var registro : registros.stream().sorted(java.util.Comparator.comparing(AsistenciaJornada::getFecha)).toList()) {
            boolean justificada = justificacionInasistenciaRepository.findByAsistenciaJornada_Id(registro.getId()).isPresent();
            if (justificada) totalJustificadas++;
            totalEquivalente = totalEquivalente.add(registro.getTipo().getEquivalencia());
            porMes.computeIfAbsent(YearMonth.from(registro.getFecha()), m -> new ArrayList<>())
                    .add(List.of(registro.getFecha().toString(), registro.getTurno().name(),
                            registro.getTipo().name(), justificada ? "Si" : "No"));
        }

        List<YearMonth> mesesDescendente = new ArrayList<>(porMes.keySet());
        java.util.Collections.reverse(mesesDescendente);
        List<PdfTableGenerator.Seccion> secciones = mesesDescendente.stream()
                .map(mes -> new PdfTableGenerator.Seccion(nombreMes(mes), columnas, porMes.get(mes)))
                .toList();

        String cicloTexto;
        String estadoTexto;
        try {
            var acl = alumnoService.cicloActualDe(alumnoId);
            cicloTexto = "Ciclo lectivo: " + acl.getCicloLectivo().getAnio();
            estadoTexto = "Estado: " + (acl.isCondicionLibre() ? "Condicion Libre" : "Regular");
        } catch (RuntimeException e) {
            cicloTexto = "";
            estadoTexto = "";
        }

        List<String> encabezado = List.of(
                "Alumno: " + alumno.getNombreCompleto(),
                cicloTexto,
                "Total de inasistencias equivalentes: " + totalEquivalente,
                "Faltas justificadas: " + totalJustificadas + " de " + registros.size(),
                estadoTexto);
        return pdfTableGenerator.generarAgrupado("Reporte de Asistencia", encabezado, secciones);
    }

    private static String nombreMes(YearMonth mes) {
        String nombre = mes.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
        return java.text.Normalizer.normalize(nombre.substring(0, 1).toUpperCase(Locale.ROOT) + nombre.substring(1), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "") + " " + mes.getYear();
    }

    /** Reporte de Alumnos en Condicion Libre (seccion 5.8.3). */
    @Transactional(readOnly = true)
    public byte[] reporteCondicionLibre() {
        TablaReporte t = construirCondicionLibre();
        return pdfTableGenerator.generar(t.titulo(), t.encabezado(), t.columnas(), t.filas());
    }

    @Transactional(readOnly = true)
    public byte[] reporteCondicionLibreExcel() {
        TablaReporte t = construirCondicionLibre();
        return excelTableGenerator.generar(t.titulo(), t.columnas(), t.filas());
    }

    private TablaReporte construirCondicionLibre() {
        CicloLectivo activo = cicloActivo();
        List<AlumnoCicloLectivo> alumnosLibres = alumnoCicloLectivoRepository.findAllByCicloLectivo_Id(activo.getId())
                .stream().filter(AlumnoCicloLectivo::isCondicionLibre).toList();

        List<String> columnas = List.of("Alumno", "Curso/Division", "Inasistencias Acumuladas", "Condicion Libre Desde");
        List<List<String>> filas = new ArrayList<>();
        for (var acl : alumnosLibres) {
            filas.add(List.of(
                    acl.getAlumno().getNombreCompleto(),
                    acl.getCursoDivision().getNombreCompleto(),
                    acl.getInasistenciasAcumuladas().toString(),
                    acl.getCondicionLibreDesde() != null ? acl.getCondicionLibreDesde().toLocalDate().toString() : "-"));
        }
        List<String> encabezado = List.of("Ciclo lectivo: " + activo.getAnio(), "Total de alumnos en Condicion Libre: " + alumnosLibres.size());
        return new TablaReporte("Alumnos en Condicion Libre", encabezado, columnas, filas);
    }

    /** Reporte Disciplinario de Curso (seccion 5.8.3): reutiliza el resumen ya calculado para la pantalla de Disciplina. */
    @Transactional(readOnly = true)
    public byte[] reporteDisciplinarioCurso(Long cursoDivisionId) {
        TablaReporte t = construirDisciplinarioCurso(cursoDivisionId);
        return pdfTableGenerator.generar(t.titulo(), t.encabezado(), t.columnas(), t.filas());
    }

    @Transactional(readOnly = true)
    public byte[] reporteDisciplinarioCursoExcel(Long cursoDivisionId) {
        TablaReporte t = construirDisciplinarioCurso(cursoDivisionId);
        return excelTableGenerator.generar(t.titulo(), t.columnas(), t.filas());
    }

    private TablaReporte construirDisciplinarioCurso(Long cursoDivisionId) {
        CursoDivision division = cursoDivisionRepository.findById(cursoDivisionId)
                .orElseThrow(() -> new BusinessException("Curso/division no encontrado."));
        List<AlumnoCicloLectivo> trayectorias = disciplinaService.resumenPorDivision(cursoDivisionId);

        List<String> columnas = List.of("Alumno", "Observaciones Acumuladas", "Firmas Acumuladas");
        List<List<String>> filas = new ArrayList<>();
        for (var acl : trayectorias) {
            filas.add(List.of(acl.getAlumno().getNombreCompleto(),
                    String.valueOf(acl.getObservacionesAcumuladas()), String.valueOf(acl.getFirmasAcumuladas())));
        }
        return new TablaReporte("Reporte Disciplinario de Curso",
                List.of("Curso/Division: " + division.getNombreCompleto()), columnas, filas);
    }

    /** Reporte de Rendimiento por Materia (seccion 5.8.3): promedio anual de cada materia dictada en la division. */
    @Transactional(readOnly = true)
    public byte[] reporteRendimientoPorMateria(Long cursoDivisionId) {
        TablaReporte t = construirRendimientoPorMateria(cursoDivisionId);
        return pdfTableGenerator.generar(t.titulo(), t.encabezado(), t.columnas(), t.filas());
    }

    @Transactional(readOnly = true)
    public byte[] reporteRendimientoPorMateriaExcel(Long cursoDivisionId) {
        TablaReporte t = construirRendimientoPorMateria(cursoDivisionId);
        return excelTableGenerator.generar(t.titulo(), t.columnas(), t.filas());
    }

    private TablaReporte construirRendimientoPorMateria(Long cursoDivisionId) {
        CursoDivision division = cursoDivisionRepository.findById(cursoDivisionId)
                .orElseThrow(() -> new BusinessException("Curso/division no encontrado."));
        List<Alumno> alumnos = alumnoService.alumnosDeDivisionEnCicloActivo(cursoDivisionId);
        List<Materia> materias = materiaRepository.findAllByActivaTrueOrderByNombre().stream()
                .filter(m -> m.getCursosDivisiones().stream().anyMatch(cd -> cd.getId().equals(cursoDivisionId)))
                .toList();

        List<String> columnas = List.of("Materia", "Promedio de la Division", "Alumnos con Datos");
        List<List<String>> filas = new ArrayList<>();
        for (Materia materia : materias) {
            BigDecimal suma = BigDecimal.ZERO;
            int conDatos = 0;
            for (Alumno alumno : alumnos) {
                BigDecimal promedio = calificacionService.promedioAnual(alumno.getId(), materia.getId());
                if (promedio != null) {
                    suma = suma.add(promedio);
                    conDatos++;
                }
            }
            String promedioDivision = conDatos > 0 ? suma.divide(BigDecimal.valueOf(conDatos), 2, RoundingMode.HALF_UP).toString() : "Sin datos";
            filas.add(List.of(materia.getNombre(), promedioDivision, conDatos + " de " + alumnos.size()));
        }
        return new TablaReporte("Reporte de Rendimiento por Materia",
                List.of("Curso/Division: " + division.getNombreCompleto()), columnas, filas);
    }

    /** Reporte de Asistencia de Curso (seccion 5.8.3): inasistencias acumuladas por alumno del curso. */
    @Transactional(readOnly = true)
    public byte[] reporteAsistenciaCurso(Long cursoDivisionId) {
        TablaReporte t = construirAsistenciaCurso(cursoDivisionId);
        return pdfTableGenerator.generar(t.titulo(), t.encabezado(), t.columnas(), t.filas());
    }

    @Transactional(readOnly = true)
    public byte[] reporteAsistenciaCursoExcel(Long cursoDivisionId) {
        TablaReporte t = construirAsistenciaCurso(cursoDivisionId);
        return excelTableGenerator.generar(t.titulo(), t.columnas(), t.filas());
    }

    private TablaReporte construirAsistenciaCurso(Long cursoDivisionId) {
        CursoDivision division = cursoDivisionRepository.findById(cursoDivisionId)
                .orElseThrow(() -> new BusinessException("Curso/division no encontrado."));
        CicloLectivo activo = cicloActivo();
        var trayectorias = alumnoCicloLectivoRepository
                .findAllByCursoDivision_IdAndCicloLectivo_IdOrderByAlumno_ApellidoAscAlumno_NombreAsc(cursoDivisionId, activo.getId());

        List<String> columnas = List.of("Alumno", "Inasistencias Acumuladas", "Condicion");
        List<List<String>> filas = new ArrayList<>();
        for (var acl : trayectorias) {
            filas.add(List.of(acl.getAlumno().getNombreCompleto(), acl.getInasistenciasAcumuladas().toString(),
                    acl.isCondicionLibre() ? "Condicion Libre" : "Regular"));
        }
        return new TablaReporte("Reporte de Asistencia de Curso",
                List.of("Curso/Division: " + division.getNombreCompleto(), "Ciclo lectivo: " + activo.getAnio()), columnas, filas);
    }

    private CicloLectivo cicloActivo() {
        return cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));
    }
}
