package ar.edu.wellspring.sigaiws.modules.workshop.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.AulaRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.WorkshopRepository;
import ar.edu.wellspring.sigaiws.modules.horario.repository.HorarioSlotRepository;
import ar.edu.wellspring.sigaiws.modules.workshop.domain.WorkshopHorarioSlot;
import ar.edu.wellspring.sigaiws.modules.workshop.dto.WorkshopSlotForm;
import ar.edu.wellspring.sigaiws.modules.workshop.repository.WorkshopDocenteRepository;
import ar.edu.wellspring.sigaiws.modules.workshop.repository.WorkshopHorarioSlotRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fase 4 (seccion 5.6): horario fijo semanal del workshop. Reutiliza el chequeo de conflictos de la seccion 5.5.3. */
@Service
@RequiredArgsConstructor
public class WorkshopHorarioService {

    private static final String MODULO = "WORKSHOPS_HORARIO";

    private final WorkshopHorarioSlotRepository workshopHorarioSlotRepository;
    private final HorarioSlotRepository horarioSlotRepository;
    private final WorkshopRepository workshopRepository;
    private final WorkshopDocenteRepository workshopDocenteRepository;
    private final AulaRepository aulaRepository;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<WorkshopHorarioSlot> gridDelWorkshop(Long workshopId) {
        CicloLectivo activo = cicloActivo();
        return workshopHorarioSlotRepository.findAllByWorkshop_IdAndCicloLectivo_Id(workshopId, activo.getId());
    }

    /** Bloques del/de los workshop(s) que dicta este docente en el ciclo activo (para "Mi Horario"). */
    @Transactional(readOnly = true)
    public List<WorkshopHorarioSlot> miHorario(Long docenteId) {
        Optional<Long> workshopId = workshopDocenteRepository.findByDocente_Id(docenteId).map(wd -> wd.getWorkshop().getId());
        if (workshopId.isEmpty()) {
            return List.of();
        }
        return gridDelWorkshop(workshopId.get());
    }

    @Transactional
    public WorkshopHorarioSlot guardarSlot(WorkshopSlotForm form) {
        CicloLectivo activo = cicloActivo();
        var workshop = workshopRepository.findById(form.getWorkshopId())
                .orElseThrow(() -> NotFoundException.of("Workshop", form.getWorkshopId()));

        WorkshopHorarioSlot slot = workshopHorarioSlotRepository
                .findByWorkshop_IdAndCicloLectivo_IdAndDiaSemanaAndPeriodoOrden(
                        form.getWorkshopId(), activo.getId(), form.getDiaSemana(), form.getPeriodoOrden())
                .orElseGet(WorkshopHorarioSlot::new);
        Long idActual = slot.getId();

        var docenteDelWorkshop = workshopDocenteRepository.findAllByWorkshop_Id(form.getWorkshopId())
                .stream().findFirst().map(wd -> wd.getDocente());
        if (docenteDelWorkshop.isPresent()) {
            horarioSlotRepository.findByDocente_IdAndCicloLectivo_IdAndDiaSemanaAndPeriodoOrdenAndIdNot(
                            docenteDelWorkshop.get().getId(), activo.getId(), form.getDiaSemana(), form.getPeriodoOrden(), -1L)
                    .ifPresent(conflicto -> {
                        throw new BusinessException("El docente del workshop ya tiene una clase asignada en ese dia y periodo, en "
                                + conflicto.getCursoDivision().getNombreCompleto() + ".");
                    });
        }

        if (form.getAulaId() != null) {
            horarioSlotRepository.findByAula_IdAndCicloLectivo_IdAndDiaSemanaAndPeriodoOrdenAndIdNot(
                            form.getAulaId(), activo.getId(), form.getDiaSemana(), form.getPeriodoOrden(), -1L)
                    .ifPresent(conflicto -> {
                        throw new BusinessException("El aula ya esta asignada a otro curso en ese dia y periodo ("
                                + conflicto.getCursoDivision().getNombreCompleto() + ").");
                    });
            workshopHorarioSlotRepository.findByAula_IdAndCicloLectivo_IdAndDiaSemanaAndPeriodoOrdenAndIdNot(
                            form.getAulaId(), activo.getId(), form.getDiaSemana(), form.getPeriodoOrden(),
                            idActual != null ? idActual : -1L)
                    .ifPresent(conflicto -> {
                        throw new BusinessException("El aula ya esta asignada a otro workshop en ese dia y periodo ("
                                + conflicto.getWorkshop().getNombre() + ").");
                    });
        }

        slot.setWorkshop(workshop);
        slot.setCicloLectivo(activo);
        slot.setDiaSemana(form.getDiaSemana());
        slot.setPeriodoOrden(form.getPeriodoOrden());
        slot.setAula(form.getAulaId() != null ? aulaRepository.findById(form.getAulaId())
                .orElseThrow(() -> NotFoundException.of("Aula", form.getAulaId())) : null);

        slot = workshopHorarioSlotRepository.save(slot);
        auditService.registrar(MODULO, "GUARDAR_SLOT_HORARIO_WORKSHOP", "WorkshopHorarioSlot", slot.getId(), null,
                "workshop=" + workshop.getNombre() + ", dia=" + slot.getDiaSemana() + ", periodo=" + slot.getPeriodoOrden());
        return slot;
    }

    private CicloLectivo cicloActivo() {
        return cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));
    }
}
