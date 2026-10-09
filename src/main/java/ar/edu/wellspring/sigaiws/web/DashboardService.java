package ar.edu.wellspring.sigaiws.web;

import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoCicloLectivo;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoCicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.service.AlumnoService;
import ar.edu.wellspring.sigaiws.modules.asistencia.domain.TipoAsistenciaJornada;
import ar.edu.wellspring.sigaiws.modules.asistencia.repository.AsistenciaJornadaRepository;
import ar.edu.wellspring.sigaiws.modules.calificacion.domain.Evaluacion;
import ar.edu.wellspring.sigaiws.modules.calificacion.repository.EvaluacionRepository;
import ar.edu.wellspring.sigaiws.modules.calificacion.repository.NotaRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.disciplina.repository.ObservacionDisciplinariaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seccion 5.9 (Dashboards Institucionales): widgets agregados que resumen el
 * estado de la institucion, separados del DashboardController para no
 * sobrecargarlo con calculos.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final BigDecimal UMBRAL_PROXIMO_DESDE = BigDecimal.valueOf(17);
    private static final BigDecimal UMBRAL_PROXIMO_HASTA = BigDecimal.valueOf(20);

    private final AsistenciaJornadaRepository asistenciaJornadaRepository;
    private final ObservacionDisciplinariaRepository observacionDisciplinariaRepository;
    private final AlumnoCicloLectivoRepository alumnoCicloLectivoRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final NotaRepository notaRepository;
    private final AlumnoService alumnoService;

    public record ResumenAsistenciaInstitucional(int porcentajePresente, long totalRegistros) {}

    /** % de registros de asistencia (asistencia_jornada) con tipo PRESENTE, sobre todo el ciclo activo hasta hoy. */
    @Transactional(readOnly = true)
    public ResumenAsistenciaInstitucional asistenciaInstitucional(CicloLectivo ciclo) {
        LocalDate hasta = LocalDate.now().isAfter(ciclo.getFechaFinC2()) ? ciclo.getFechaFinC2() : LocalDate.now();
        long total = asistenciaJornadaRepository.countByFechaBetween(ciclo.getFechaInicioC1(), hasta);
        if (total == 0) {
            return new ResumenAsistenciaInstitucional(0, 0);
        }
        long presentes = asistenciaJornadaRepository.countByFechaBetweenAndTipo(ciclo.getFechaInicioC1(), hasta, TipoAsistenciaJornada.PRESENTE);
        return new ResumenAsistenciaInstitucional((int) Math.round(presentes * 100.0 / total), total);
    }

    /** Nivel de urgencia para el color del widget: NORMAL (>=15 dias), PROXIMO (7-14 dias), URGENTE (<7 dias o vencido). */
    public enum UrgenciaCuatrimestre { NORMAL, PROXIMO, URGENTE }

    public record CountdownCuatrimestre(int cuatrimestre, LocalDate fechaCierre, boolean cerrado,
                                         boolean vencido, long diasRestantesOVencidos, UrgenciaCuatrimestre urgencia) {}

    /**
     * Estado del cierre del cuatrimestre en curso: si ya se cerro, cuantos dias faltan, o si la
     * fecha de cierre ya paso y todavia nadie lo cerro manualmente (caso "vencido", el mas urgente).
     */
    @Transactional(readOnly = true)
    public CountdownCuatrimestre countdownCierreCuatrimestre(CicloLectivo ciclo) {
        int cuatrimestre = ciclo.getCuatrimestreEnCurso();
        boolean cerrado = cuatrimestre == 1 ? ciclo.isCuatrimestre1Cerrado() : ciclo.isCuatrimestre2Cerrado();
        LocalDate fechaCierre = cuatrimestre == 1 ? ciclo.getFechaFinC1() : ciclo.getFechaFinC2();
        if (cerrado) {
            return new CountdownCuatrimestre(cuatrimestre, fechaCierre, true, false, 0, UrgenciaCuatrimestre.NORMAL);
        }
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), fechaCierre);
        boolean vencido = dias < 0;
        UrgenciaCuatrimestre urgencia = vencido || dias < 7 ? UrgenciaCuatrimestre.URGENTE
                : dias < 15 ? UrgenciaCuatrimestre.PROXIMO : UrgenciaCuatrimestre.NORMAL;
        return new CountdownCuatrimestre(cuatrimestre, fechaCierre, false, vencido, Math.abs(dias), urgencia);
    }

    public record MetricasDisciplinariasSemanales(int total, Map<String, Long> porCategoria) {}

    /** Observaciones disciplinarias registradas (institucional) en los ultimos 7 dias, con desglose por categoria. */
    @Transactional(readOnly = true)
    public MetricasDisciplinariasSemanales disciplinaUltimos7Dias() {
        var observaciones = observacionDisciplinariaRepository.findAllByFechaGreaterThanEqual(LocalDate.now().minusDays(7));
        Map<String, Long> porCategoria = observaciones.stream()
                .collect(Collectors.groupingBy(o -> o.getCategoria() != null ? o.getCategoria() : "Sin categoria",
                        LinkedHashMap::new, Collectors.counting()));
        return new MetricasDisciplinariasSemanales(observaciones.size(), porCategoria);
    }

    /** Alumnos regulares con entre 17 y 20 faltas acumuladas en el ciclo activo: se acercan a Condicion Libre sin haber llegado todavia. */
    @Transactional(readOnly = true)
    public List<Alumno> alumnosProximosAlUmbral(CicloLectivo ciclo) {
        return alumnoCicloLectivoRepository.findAllByCicloLectivo_Id(ciclo.getId()).stream()
                .filter(acl -> !acl.isCondicionLibre())
                .filter(acl -> acl.getInasistenciasAcumuladas().compareTo(UMBRAL_PROXIMO_DESDE) >= 0
                        && acl.getInasistenciasAcumuladas().compareTo(UMBRAL_PROXIMO_HASTA) <= 0)
                .sorted((a, b) -> b.getInasistenciasAcumuladas().compareTo(a.getInasistenciasAcumuladas()))
                .map(AlumnoCicloLectivo::getAlumno)
                .toList();
    }

    public record EvaluacionSinCerrar(Evaluacion evaluacion, int notasCargadas, int totalAlumnos) {}

    /** Evaluaciones del docente en el cuatrimestre en curso a las que todavia les falta la nota de algun alumno del curso. */
    @Transactional(readOnly = true)
    public List<EvaluacionSinCerrar> evaluacionesSinCerrar(Long docenteId, CicloLectivo ciclo) {
        int cuatrimestre = ciclo.getCuatrimestreEnCurso();
        List<EvaluacionSinCerrar> resultado = new ArrayList<>();
        for (Evaluacion evaluacion : evaluacionRepository.findAllByCreadoPor_IdAndCicloLectivo_IdAndCuatrimestre(
                docenteId, ciclo.getId(), cuatrimestre)) {
            int totalAlumnos = alumnoService.alumnosDeDivisionEnCicloActivo(evaluacion.getCursoDivision().getId()).size();
            int notasCargadas = notaRepository.findAllByEvaluacion_Id(evaluacion.getId()).size();
            if (notasCargadas < totalAlumnos) {
                resultado.add(new EvaluacionSinCerrar(evaluacion, notasCargadas, totalAlumnos));
            }
        }
        return resultado;
    }
}
