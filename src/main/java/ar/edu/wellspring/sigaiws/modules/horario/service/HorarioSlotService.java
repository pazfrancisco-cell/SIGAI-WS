package ar.edu.wellspring.sigaiws.modules.horario.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.AulaRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.MateriaRepository;
import ar.edu.wellspring.sigaiws.modules.horario.domain.HorarioSlot;
import ar.edu.wellspring.sigaiws.modules.horario.dto.SlotForm;
import ar.edu.wellspring.sigaiws.modules.horario.repository.HorarioSlotRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seccion 5.5.3: "Al guardar, el sistema verifica conflictos: que el docente
 * no este asignado en otro curso en ese mismo periodo, que el aula no este
 * asignada a otro curso. Si hay conflicto, muestra alerta especifica."
 */
@Service
@RequiredArgsConstructor
public class HorarioSlotService {

    private static final String MODULO = "HORARIOS";

    private final HorarioSlotRepository horarioSlotRepository;
    private final MateriaRepository materiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AulaRepository aulaRepository;
    private final CursoDivisionRepository cursoDivisionRepository;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<HorarioSlot> gridDeDivision(Long cursoDivisionId) {
        CicloLectivo activo = cicloActivo();
        return horarioSlotRepository.findAllByCursoDivision_IdAndCicloLectivo_Id(cursoDivisionId, activo.getId());
    }

    @Transactional(readOnly = true)
    public List<HorarioSlot> miHorario(Long docenteId) {
        CicloLectivo activo = cicloActivo();
        return horarioSlotRepository.findAllByDocente_IdAndCicloLectivo_Id(docenteId, activo.getId());
    }

    @Transactional
    public HorarioSlot guardarSlot(SlotForm form) {
        CicloLectivo activo = cicloActivo();

        HorarioSlot slot = horarioSlotRepository
                .findByCursoDivision_IdAndCicloLectivo_IdAndDiaSemanaAndPeriodoOrden(
                        form.getCursoDivisionId(), activo.getId(), form.getDiaSemana(), form.getPeriodoOrden())
                .orElseGet(HorarioSlot::new);

        Long idActual = slot.getId(); // null si es un slot nuevo, se excluye de los chequeos de conflicto

        if (form.getDocenteId() != null) {
            horarioSlotRepository.findByDocente_IdAndCicloLectivo_IdAndDiaSemanaAndPeriodoOrdenAndIdNot(
                            form.getDocenteId(), activo.getId(), form.getDiaSemana(), form.getPeriodoOrden(),
                            idActual != null ? idActual : -1L)
                    .ifPresent(conflicto -> {
                        throw new BusinessException("El docente ya tiene una clase asignada en ese dia y periodo, en "
                                + conflicto.getCursoDivision().getNombreCompleto() + ".");
                    });
        }
        if (form.getAulaId() != null) {
            horarioSlotRepository.findByAula_IdAndCicloLectivo_IdAndDiaSemanaAndPeriodoOrdenAndIdNot(
                            form.getAulaId(), activo.getId(), form.getDiaSemana(), form.getPeriodoOrden(),
                            idActual != null ? idActual : -1L)
                    .ifPresent(conflicto -> {
                        throw new BusinessException("El aula ya esta asignada a otro curso en ese dia y periodo ("
                                + conflicto.getCursoDivision().getNombreCompleto() + ").");
                    });
        }

        slot.setCursoDivision(cursoDivisionRepository.findById(form.getCursoDivisionId())
                .orElseThrow(() -> NotFoundException.of("CursoDivision", form.getCursoDivisionId())));
        slot.setCicloLectivo(activo);
        slot.setDiaSemana(form.getDiaSemana());
        slot.setPeriodoOrden(form.getPeriodoOrden());
        slot.setMateria(form.getMateriaId() != null ? materiaRepository.findById(form.getMateriaId())
                .orElseThrow(() -> NotFoundException.of("Materia", form.getMateriaId())) : null);
        slot.setDocente(form.getDocenteId() != null ? usuarioRepository.findById(form.getDocenteId())
                .orElseThrow(() -> NotFoundException.of("Usuario", form.getDocenteId())) : null);
        slot.setAula(form.getAulaId() != null ? aulaRepository.findById(form.getAulaId())
                .orElseThrow(() -> NotFoundException.of("Aula", form.getAulaId())) : null);

        slot = horarioSlotRepository.save(slot);
        auditService.registrar(MODULO, "GUARDAR_SLOT_HORARIO", "HorarioSlot", slot.getId(), null,
                "dia=" + slot.getDiaSemana() + ", periodo=" + slot.getPeriodoOrden());
        return slot;
    }

    private CicloLectivo cicloActivo() {
        return cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));
    }
}
