package ar.edu.wellspring.sigaiws.modules.workshop.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.Workshop;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.WorkshopRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import ar.edu.wellspring.sigaiws.modules.workshop.domain.WorkshopAsistencia;
import ar.edu.wellspring.sigaiws.modules.workshop.dto.WorkshopAsistenciaBatchForm;
import ar.edu.wellspring.sigaiws.modules.workshop.repository.WorkshopAsistenciaRepository;
import ar.edu.wellspring.sigaiws.modules.workshop.repository.WorkshopDocenteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fase 4 (seccion 5.6): asistencia de workshop, clase por clase, con roster dinamico segun el cuatrimestre. */
@Service
@RequiredArgsConstructor
public class WorkshopAsistenciaService {

    private static final String MODULO = "WORKSHOPS_ASISTENCIA";

    private final WorkshopAsistenciaRepository workshopAsistenciaRepository;
    private final WorkshopAsignacionService workshopAsignacionService;
    private final WorkshopRepository workshopRepository;
    private final WorkshopDocenteRepository workshopDocenteRepository;
    private final AlumnoRepository alumnoRepository;
    private final AuditService auditService;

    /** Roster del workshop para el cuatrimestre indicado: quienes lo eligieron ese cuatrimestre (5.6.3/5.6.4). */
    @Transactional(readOnly = true)
    public List<Alumno> alumnosDelWorkshop(Long workshopId, int cuatrimestre) {
        return workshopAsignacionService.alumnosDelWorkshop(workshopId, cuatrimestre).stream()
                .map(acl -> acl.getAlumno())
                .distinct()
                .toList();
    }

    @Transactional
    public void registrarClase(WorkshopAsistenciaBatchForm form, Usuario registradoPor) {
        Workshop workshop = workshopRepository.findById(form.getWorkshopId())
                .orElseThrow(() -> NotFoundException.of("Workshop", form.getWorkshopId()));
        verificarPropioWorkshop(form.getWorkshopId(), registradoPor);

        for (WorkshopAsistenciaBatchForm.Item item : form.getItems()) {
            WorkshopAsistencia registro = workshopAsistenciaRepository
                    .findByAlumno_IdAndWorkshop_IdAndFecha(item.getAlumnoId(), form.getWorkshopId(), form.getFecha())
                    .orElseGet(WorkshopAsistencia::new);
            Alumno alumno = alumnoRepository.findById(item.getAlumnoId())
                    .orElseThrow(() -> NotFoundException.of("Alumno", item.getAlumnoId()));
            registro.setAlumno(alumno);
            registro.setWorkshop(workshop);
            registro.setFecha(form.getFecha());
            registro.setTipo(item.getTipo());
            registro.setObservacion(item.getObservacion());
            registro.setRegistradoPor(registradoPor);
            workshopAsistenciaRepository.save(registro);
        }
        auditService.registrar(MODULO, "REGISTRAR_ASISTENCIA_WORKSHOP", "WorkshopAsistencia", null, null,
                "workshop=" + workshop.getNombre() + ", fecha=" + form.getFecha());
    }

    /** Defensa en profundidad: un docente solo puede tomar asistencia de SU workshop asignado. */
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
}
