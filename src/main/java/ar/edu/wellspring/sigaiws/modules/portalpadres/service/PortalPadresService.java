package ar.edu.wellspring.sigaiws.modules.portalpadres.service;

import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoCicloLectivo;
import ar.edu.wellspring.sigaiws.modules.asistencia.domain.AsistenciaJornada;
import ar.edu.wellspring.sigaiws.modules.asistencia.domain.TipoAsistenciaJornada;
import ar.edu.wellspring.sigaiws.modules.asistencia.repository.AsistenciaJornadaRepository;
import ar.edu.wellspring.sigaiws.modules.asistencia.repository.JustificacionInasistenciaRepository;
import ar.edu.wellspring.sigaiws.modules.calificacion.domain.TipoEvaluacion;
import ar.edu.wellspring.sigaiws.modules.calificacion.service.CalificacionService;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.Materia;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.MateriaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Modulo 5.11 (Portal de Padres): arma el detalle academico de un hijo/a
 * (notas por materia, dias con falta y su motivo). El control de acceso
 * (que el alumno este vinculado al tutor) lo hace el controller.
 */
@Service
@RequiredArgsConstructor
public class PortalPadresService {

    private final CalificacionService calificacionService;
    private final MateriaRepository materiaRepository;
    private final AsistenciaJornadaRepository asistenciaJornadaRepository;
    private final JustificacionInasistenciaRepository justificacionRepository;

    public record NotaDetalle(TipoEvaluacion tipo, LocalDate fecha, String titulo, BigDecimal valor) {}

    public record MateriaDetalle(String materia, List<NotaDetalle> notasC1, BigDecimal promedioC1,
                                 List<NotaDetalle> notasC2, BigDecimal promedioC2,
                                 BigDecimal promedioAnual, boolean aprobada) {}

    /** Todas las notas del ciclo activo, materia por materia y separadas por cuatrimestre, con sus promedios. */
    @Transactional(readOnly = true)
    public List<MateriaDetalle> calificacionesDetalladas(Long alumnoId, AlumnoCicloLectivo cicloActual) {
        Long cursoDivisionId = cicloActual.getCursoDivision().getId();
        List<MateriaDetalle> resultado = new ArrayList<>();
        for (Materia materia : materiaRepository.findAllByActivaTrueOrderByNombre()) {
            if (materia.getCursosDivisiones().stream().noneMatch(cd -> cd.getId().equals(cursoDivisionId))) {
                continue;
            }
            BigDecimal anual = calificacionService.promedioAnual(alumnoId, materia.getId());
            resultado.add(new MateriaDetalle(
                    materia.getNombre(),
                    notas(alumnoId, materia.getId(), 1),
                    calificacionService.promedioCuatrimestral(alumnoId, materia.getId(), 1),
                    notas(alumnoId, materia.getId(), 2),
                    calificacionService.promedioCuatrimestral(alumnoId, materia.getId(), 2),
                    anual,
                    anual != null && calificacionService.estaAprobada(anual)));
        }
        return resultado;
    }

    private List<NotaDetalle> notas(Long alumnoId, Long materiaId, int cuatrimestre) {
        return calificacionService.notasDe(alumnoId, materiaId, cuatrimestre).stream()
                .map(n -> new NotaDetalle(n.getEvaluacion().getTipo(), n.getEvaluacion().getFecha(),
                        n.getEvaluacion().getTitulo(), n.getValor()))
                .toList();
    }

    /** Un turno con falta: tipo (motivo), cuanto vale y, si se justifico, el motivo de la justificacion. */
    public record MotivoFalta(AsistenciaJornada.Turno turno, TipoAsistenciaJornada tipo, String valor,
                              boolean justificada, String tipoJustificacion, String descripcionJustificacion) {}

    /** Un dia con alguna falta: total del dia (sumando manana y tarde) y el detalle de cada turno. */
    public record DiaConFalta(LocalDate fecha, String totalDia, List<MotivoFalta> motivos) {}

    /** Dias del ciclo activo en los que el alumno sumo falta (1, 1/2, 1/4...), del mas reciente al mas antiguo. */
    @Transactional(readOnly = true)
    public List<DiaConFalta> diasConFalta(Long alumnoId, CicloLectivo ciclo) {
        List<AsistenciaJornada> registros = asistenciaJornadaRepository
                .findAllByAlumno_IdAndFechaBetweenOrderByFecha(alumnoId, ciclo.getFechaInicioC1(), ciclo.getFechaFinC2());

        Map<LocalDate, List<AsistenciaJornada>> porDia = new TreeMap<>(Comparator.reverseOrder());
        for (AsistenciaJornada r : registros) {
            if (r.getTipo().getEquivalencia().signum() > 0) {
                porDia.computeIfAbsent(r.getFecha(), f -> new ArrayList<>()).add(r);
            }
        }

        List<DiaConFalta> dias = new ArrayList<>();
        for (var entrada : porDia.entrySet()) {
            BigDecimal total = BigDecimal.ZERO;
            List<MotivoFalta> motivos = new ArrayList<>();
            for (AsistenciaJornada r : entrada.getValue().stream().sorted(Comparator.comparing(AsistenciaJornada::getTurno)).toList()) {
                total = total.add(r.getTipo().getEquivalencia());
                var justificacion = justificacionRepository.findByAsistenciaJornada_Id(r.getId());
                motivos.add(new MotivoFalta(r.getTurno(), r.getTipo(), fraccion(r.getTipo().getEquivalencia()),
                        justificacion.isPresent() || r.getTipo() == TipoAsistenciaJornada.AUSENTE_JUSTIFICADO,
                        justificacion.map(j -> j.getTipo().name()).orElse(null),
                        justificacion.map(j -> j.getDescripcion()).orElse(null)));
            }
            dias.add(new DiaConFalta(entrada.getKey(), fraccion(total), motivos));
        }
        return dias;
    }

    /** 0.25 -> "1/4", 0.5 -> "1/2", 1 -> "1", 1.25 -> "1 1/4". */
    static String fraccion(BigDecimal valor) {
        int cuartos = valor.multiply(BigDecimal.valueOf(4)).intValue();
        int enteros = cuartos / 4;
        String resto = switch (cuartos % 4) {
            case 1 -> "1/4";
            case 2 -> "1/2";
            case 3 -> "3/4";
            default -> "";
        };
        if (enteros == 0) {
            return resto.isEmpty() ? "0" : resto;
        }
        return resto.isEmpty() ? String.valueOf(enteros) : enteros + " " + resto;
    }
}
