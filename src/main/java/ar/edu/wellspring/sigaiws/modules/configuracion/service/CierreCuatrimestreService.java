package ar.edu.wellspring.sigaiws.modules.configuracion.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.modules.alerta.domain.Alerta;
import ar.edu.wellspring.sigaiws.modules.alerta.service.AlertaService;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.alumno.service.AlumnoService;
import ar.edu.wellspring.sigaiws.modules.calificacion.domain.Evaluacion;
import ar.edu.wellspring.sigaiws.modules.calificacion.repository.EvaluacionRepository;
import ar.edu.wellspring.sigaiws.modules.calificacion.repository.NotaRepository;
import ar.edu.wellspring.sigaiws.modules.calificacion.service.CalificacionService;
import ar.edu.wellspring.sigaiws.modules.calificacion.service.MesaExamenService;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CursoDivision;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.Materia;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.horario.domain.AsignacionDocente;
import ar.edu.wellspring.sigaiws.modules.horario.repository.AsignacionDocenteRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seccion 6.4 (Flujo: Cierre de Cuatrimestre). Verificacion previa (docentes
 * sin ninguna evaluacion cargada, alumnos con evaluaciones sin nota) antes de
 * bloquear la carga directa; identificacion de alumnos con promedio anual
 * &lt;7 al cerrar el segundo cuatrimestre (instancia de recuperacion).
 */
@Service
@RequiredArgsConstructor
public class CierreCuatrimestreService {

    private static final String MODULO = "CONFIGURACION";

    public record DocenteSinCarga(Usuario docente, Materia materia, CursoDivision division) {}

    public record AlumnoSinNotaCompleta(Alumno alumno, Materia materia, CursoDivision division) {}

    public record AlumnoParaRecuperacion(Alumno alumno, Materia materia, BigDecimal promedioAnual) {}

    public record VerificacionPrevia(List<DocenteSinCarga> docentesSinCarga, List<AlumnoSinNotaCompleta> alumnosSinNotas) {}

    private final CicloLectivoRepository cicloLectivoRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final NotaRepository notaRepository;
    private final AlumnoService alumnoService;
    private final CalificacionService calificacionService;
    private final AlertaService alertaService;
    private final MesaExamenService mesaExamenService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public VerificacionPrevia verificar(int cuatrimestre) {
        CicloLectivo activo = cicloActivo();
        List<AsignacionDocente> asignaciones = asignacionDocenteRepository.findAllByCicloLectivo_IdOrderByDocente_NombreCompleto(activo.getId());

        List<DocenteSinCarga> docentesSinCarga = new ArrayList<>();
        List<AlumnoSinNotaCompleta> alumnosSinNotas = new ArrayList<>();

        for (AsignacionDocente asignacion : asignaciones) {
            List<Evaluacion> evaluaciones = evaluacionRepository.findAllByMateria_IdAndCursoDivision_IdAndCicloLectivo_IdAndCuatrimestreOrderByFecha(
                    asignacion.getMateria().getId(), asignacion.getCursoDivision().getId(), activo.getId(), cuatrimestre);

            if (evaluaciones.isEmpty()) {
                docentesSinCarga.add(new DocenteSinCarga(asignacion.getDocente(), asignacion.getMateria(), asignacion.getCursoDivision()));
                continue;
            }

            List<Alumno> alumnos = alumnoService.alumnosDeDivisionEnCicloActivo(asignacion.getCursoDivision().getId());
            for (Alumno alumno : alumnos) {
                boolean faltaAlguna = evaluaciones.stream()
                        .anyMatch(ev -> notaRepository.findByEvaluacion_IdAndAlumno_Id(ev.getId(), alumno.getId()).isEmpty());
                if (faltaAlguna) {
                    alumnosSinNotas.add(new AlumnoSinNotaCompleta(alumno, asignacion.getMateria(), asignacion.getCursoDivision()));
                }
            }
        }
        return new VerificacionPrevia(docentesSinCarga, alumnosSinNotas);
    }

    @Transactional
    public List<AlumnoParaRecuperacion> cerrar(int cuatrimestre) {
        if (cuatrimestre != 1 && cuatrimestre != 2) {
            throw new BusinessException("El cuatrimestre debe ser 1 o 2.");
        }
        CicloLectivo activo = cicloActivo();
        if (cuatrimestre == 1) {
            activo.setCuatrimestre1Cerrado(true);
        } else {
            activo.setCuatrimestre2Cerrado(true);
        }
        cicloLectivoRepository.save(activo);
        auditService.registrar(MODULO, "CERRAR_CUATRIMESTRE", "CicloLectivo", activo.getId(), null, cuatrimestre);

        List<AlumnoParaRecuperacion> paraRecuperacion = new ArrayList<>();
        if (cuatrimestre == 2) {
            paraRecuperacion = identificarParaRecuperacion(activo);
            // Las materias desaprobadas quedan adeudadas para las mesas de diciembre y febrero.
            mesaExamenService.sincronizarCiclo(activo);
        }
        return paraRecuperacion;
    }

    /** Seccion 6.4 paso 4: "se identifican los alumnos con promedios anuales < 7 para instancia de recuperacion". */
    private List<AlumnoParaRecuperacion> identificarParaRecuperacion(CicloLectivo activo) {
        List<AlumnoParaRecuperacion> resultado = new ArrayList<>();
        List<AsignacionDocente> asignaciones = asignacionDocenteRepository.findAllByCicloLectivo_IdOrderByDocente_NombreCompleto(activo.getId());

        Set<String> combosVistos = new LinkedHashSet<>();
        for (AsignacionDocente asignacion : asignaciones) {
            String clave = asignacion.getMateria().getId() + "-" + asignacion.getCursoDivision().getId();
            if (!combosVistos.add(clave)) {
                continue;
            }
            List<Alumno> alumnos = alumnoService.alumnosDeDivisionEnCicloActivo(asignacion.getCursoDivision().getId());
            for (Alumno alumno : alumnos) {
                BigDecimal promedio = calificacionService.promedioAnual(alumno.getId(), asignacion.getMateria().getId());
                if (promedio != null && !calificacionService.estaAprobada(promedio)) {
                    resultado.add(new AlumnoParaRecuperacion(alumno, asignacion.getMateria(), promedio));
                    alertaService.crear("Instancia de recuperacion", Alerta.Prioridad.ALTA, alumno,
                            "%s no alcanzo el promedio minimo en %s (promedio anual: %s). Corresponde instancia de recuperacion en diciembre."
                                    .formatted(alumno.getNombreCompleto(), asignacion.getMateria().getNombre(), promedio));
                }
            }
        }
        return resultado;
    }

    private CicloLectivo cicloActivo() {
        return cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));
    }
}
