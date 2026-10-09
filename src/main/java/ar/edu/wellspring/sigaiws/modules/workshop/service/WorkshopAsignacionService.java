package ar.edu.wellspring.sigaiws.modules.workshop.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoCicloLectivo;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoCicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.Workshop;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.WorkshopBanda;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.WorkshopRepository;
import ar.edu.wellspring.sigaiws.modules.workshop.domain.WorkshopDocente;
import ar.edu.wellspring.sigaiws.modules.workshop.repository.WorkshopDocenteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Modulo 5.6 (Workshops): asignacion de docentes y alumnos, cambio obligatorio C1->C2. */
@Service
@RequiredArgsConstructor
public class WorkshopAsignacionService {

    private static final String MODULO = "WORKSHOPS";

    private final WorkshopRepository workshopRepository;
    private final WorkshopDocenteRepository workshopDocenteRepository;
    private final AlumnoCicloLectivoRepository alumnoCicloLectivoRepository;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<AlumnoCicloLectivo> alumnosDelWorkshop(Long workshopId, int cuatrimestre) {
        CicloLectivo activo = cicloActivo();
        return cuatrimestre == 1
                ? alumnoCicloLectivoRepository.findAllByWorkshopC1_IdAndCicloLectivo_Id(workshopId, activo.getId())
                : alumnoCicloLectivoRepository.findAllByWorkshopC2_IdAndCicloLectivo_Id(workshopId, activo.getId());
    }

    @Transactional(readOnly = true)
    public List<AlumnoCicloLectivo> alumnosSinWorkshopC2(Long cursoDivisionId) {
        Long cicloId = cicloActivo().getId();
        return cursoDivisionId == null
                ? alumnoCicloLectivoRepository.findAllByCicloLectivo_IdAndWorkshopC2IsNull(cicloId)
                : alumnoCicloLectivoRepository.findAllByCicloLectivo_IdAndCursoDivision_IdAndWorkshopC2IsNullOrderByAlumno_ApellidoAscAlumno_NombreAsc(
                        cicloId, cursoDivisionId);
    }

    @Transactional(readOnly = true)
    public List<AlumnoCicloLectivo> alumnosSinWorkshopC1(Long cursoDivisionId) {
        Long cicloId = cicloActivo().getId();
        return cursoDivisionId == null
                ? alumnoCicloLectivoRepository.findAllByCicloLectivo_IdAndWorkshopC1IsNull(cicloId)
                : alumnoCicloLectivoRepository.findAllByCicloLectivo_IdAndCursoDivision_IdAndWorkshopC1IsNullOrderByAlumno_ApellidoAscAlumno_NombreAsc(
                        cicloId, cursoDivisionId);
    }

    /** Seccion 5.6.2: "Un docente puede estar asignado a un unico workshop." */
    @Transactional
    public void asignarDocente(Long workshopId, Long docenteId) {
        if (workshopDocenteRepository.existsByDocente_Id(docenteId)) {
            throw new BusinessException("Ese docente ya esta asignado a otro workshop.");
        }
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> NotFoundException.of("Workshop", workshopId));
        WorkshopDocente asignacion = new WorkshopDocente();
        asignacion.setWorkshop(workshop);
        ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario docente = new ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario();
        docente.setId(docenteId);
        asignacion.setDocente(docente);
        workshopDocenteRepository.save(asignacion);
        auditService.registrar(MODULO, "ASIGNAR_DOCENTE_WORKSHOP", "WorkshopDocente", asignacion.getId(), null, workshop.getNombre());
    }

    @Transactional
    public void asignarAlumnoC1(Long alumnoCicloLectivoId, Long workshopId) {
        AlumnoCicloLectivo acl = obtener(alumnoCicloLectivoId);
        Workshop workshop = workshopRepository.findById(workshopId).orElseThrow(() -> NotFoundException.of("Workshop", workshopId));
        verificarBanda(acl, workshop);
        acl.setWorkshopC1(workshop);
        alumnoCicloLectivoRepository.save(acl);
        auditService.registrar(MODULO, "ASIGNAR_WORKSHOP_C1", "AlumnoCicloLectivo", acl.getId(), null, workshopId);
    }

    /** Seccion 5.6.4: "el mismo no esta disponible para seleccion, garantizando el cambio obligatorio". */
    @Transactional
    public void asignarAlumnoC2(Long alumnoCicloLectivoId, Long workshopId) {
        AlumnoCicloLectivo acl = obtener(alumnoCicloLectivoId);
        if (acl.getWorkshopC1() != null && acl.getWorkshopC1().getId().equals(workshopId)) {
            throw new BusinessException("El alumno debe cambiar de workshop: no puede repetir el del Primer Cuatrimestre.");
        }
        Workshop workshop = workshopRepository.findById(workshopId).orElseThrow(() -> NotFoundException.of("Workshop", workshopId));
        verificarBanda(acl, workshop);
        acl.setWorkshopC2(workshop);
        alumnoCicloLectivoRepository.save(acl);
        auditService.registrar(MODULO, "ASIGNAR_WORKSHOP_C2", "AlumnoCicloLectivo", acl.getId(), null, workshopId);
    }

    /** Seccion 5.6: un alumno solo puede elegir workshops de su propia banda de años (1-3 o 4-6). */
    private void verificarBanda(AlumnoCicloLectivo acl, Workshop workshop) {
        WorkshopBanda bandaAlumno = WorkshopBanda.deAnio(acl.getCursoDivision().getAnio());
        if (workshop.getBanda() != bandaAlumno) {
            throw new BusinessException("Ese workshop no corresponde a la banda de años del alumno.");
        }
    }

    private AlumnoCicloLectivo obtener(Long id) {
        return alumnoCicloLectivoRepository.findById(id).orElseThrow(() -> NotFoundException.of("AlumnoCicloLectivo", id));
    }

    private CicloLectivo cicloActivo() {
        return cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));
    }
}
