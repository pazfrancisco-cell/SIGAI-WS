package ar.edu.wellspring.sigaiws.modules.workshop.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.WorkshopRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import ar.edu.wellspring.sigaiws.modules.workshop.domain.WorkshopEvaluacion;
import ar.edu.wellspring.sigaiws.modules.workshop.domain.WorkshopNota;
import ar.edu.wellspring.sigaiws.modules.workshop.dto.WorkshopCargaNotasForm;
import ar.edu.wellspring.sigaiws.modules.workshop.dto.WorkshopEvaluacionForm;
import ar.edu.wellspring.sigaiws.modules.workshop.repository.WorkshopDocenteRepository;
import ar.edu.wellspring.sigaiws.modules.workshop.repository.WorkshopEvaluacionRepository;
import ar.edu.wellspring.sigaiws.modules.workshop.repository.WorkshopNotaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fase 4 (seccion 5.6): calificaciones de workshop, evaluacion a texto libre (a criterio de cada docente). */
@Service
@RequiredArgsConstructor
public class WorkshopCalificacionService {

    private static final String MODULO = "WORKSHOPS_CALIFICACIONES";

    private final WorkshopEvaluacionRepository workshopEvaluacionRepository;
    private final WorkshopNotaRepository workshopNotaRepository;
    private final WorkshopRepository workshopRepository;
    private final WorkshopDocenteRepository workshopDocenteRepository;
    private final AlumnoRepository alumnoRepository;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<WorkshopEvaluacion> evaluacionesDe(Long workshopId, int cuatrimestre) {
        CicloLectivo activo = cicloActivo();
        return workshopEvaluacionRepository.findAllByWorkshop_IdAndCicloLectivo_IdAndCuatrimestreOrderByFecha(
                workshopId, activo.getId(), cuatrimestre);
    }

    @Transactional
    public WorkshopEvaluacion crearEvaluacion(WorkshopEvaluacionForm form, Usuario creadoPor) {
        if (form.getFecha().isAfter(java.time.LocalDate.now())) {
            throw new BusinessException("La fecha de la evaluacion no puede ser futura.");
        }
        verificarPropioWorkshop(form.getWorkshopId(), creadoPor);
        verificarCuatrimestreAbierto(cicloActivo(), form.getCuatrimestre());

        WorkshopEvaluacion evaluacion = new WorkshopEvaluacion();
        evaluacion.setWorkshop(workshopRepository.findById(form.getWorkshopId())
                .orElseThrow(() -> NotFoundException.of("Workshop", form.getWorkshopId())));
        evaluacion.setCicloLectivo(cicloActivo());
        evaluacion.setCuatrimestre(form.getCuatrimestre());
        evaluacion.setTitulo(form.getTitulo());
        evaluacion.setFecha(form.getFecha());
        evaluacion.setCreadoPor(creadoPor);
        evaluacion = workshopEvaluacionRepository.save(evaluacion);
        auditService.registrar(MODULO, "CREAR_EVALUACION_WORKSHOP", "WorkshopEvaluacion", evaluacion.getId(), null, form.getTitulo());
        return evaluacion;
    }

    @Transactional
    public void cargarNotas(WorkshopCargaNotasForm form, Usuario cargadoPor) {
        WorkshopEvaluacion evaluacion = workshopEvaluacionRepository.findById(form.getEvaluacionId())
                .orElseThrow(() -> NotFoundException.of("WorkshopEvaluacion", form.getEvaluacionId()));
        verificarPropioWorkshop(evaluacion.getWorkshop().getId(), cargadoPor);
        verificarCuatrimestreAbierto(evaluacion.getCicloLectivo(), evaluacion.getCuatrimestre());

        for (WorkshopCargaNotasForm.Item item : form.getItems()) {
            if (item.getTexto() == null || item.getTexto().isBlank()) {
                continue;
            }
            WorkshopNota nota = workshopNotaRepository.findByEvaluacion_IdAndAlumno_Id(evaluacion.getId(), item.getAlumnoId())
                    .orElseGet(WorkshopNota::new);
            Alumno alumno = alumnoRepository.findById(item.getAlumnoId())
                    .orElseThrow(() -> NotFoundException.of("Alumno", item.getAlumnoId()));
            nota.setEvaluacion(evaluacion);
            nota.setAlumno(alumno);
            nota.setTexto(item.getTexto());
            nota.setCargadoPor(cargadoPor);
            workshopNotaRepository.save(nota);
        }
        auditService.registrar(MODULO, "CARGAR_NOTAS_WORKSHOP", "WorkshopEvaluacion", evaluacion.getId(), null,
                "alumnos=" + form.getItems().size());
    }

    /** Defensa en profundidad: un docente solo puede cargar evaluaciones/notas de SU workshop asignado. */
    private void verificarPropioWorkshop(Long workshopId, Usuario usuario) {
        if (!usuario.tieneRol(RolNombre.DOCENTE)) {
            return;
        }
        boolean esSuWorkshop = workshopDocenteRepository.findByDocente_Id(usuario.getId())
                .map(wd -> wd.getWorkshop().getId().equals(workshopId))
                .orElse(false);
        if (!esSuWorkshop) {
            throw new BusinessException("No estas asignado a ese workshop.");
        }
    }

    private CicloLectivo cicloActivo() {
        return cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));
    }

    /** Seccion 6.4 (Cierre de Cuatrimestre): misma regla que para materias, seccion 5.3.6. */
    private void verificarCuatrimestreAbierto(CicloLectivo ciclo, int cuatrimestre) {
        boolean cerrado = cuatrimestre == 1 ? ciclo.isCuatrimestre1Cerrado() : ciclo.isCuatrimestre2Cerrado();
        if (cerrado) {
            throw new BusinessException("El cuatrimestre " + cuatrimestre + " esta cerrado. No se pueden agregar ni editar evaluaciones de workshop.");
        }
    }
}
