package ar.edu.wellspring.sigaiws.modules.workshop.repository;

import ar.edu.wellspring.sigaiws.modules.workshop.domain.WorkshopHorarioSlot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkshopHorarioSlotRepository extends JpaRepository<WorkshopHorarioSlot, Long> {

    List<WorkshopHorarioSlot> findAllByWorkshop_IdAndCicloLectivo_Id(Long workshopId, Long cicloLectivoId);

    Optional<WorkshopHorarioSlot> findByWorkshop_IdAndCicloLectivo_IdAndDiaSemanaAndPeriodoOrden(
            Long workshopId, Long cicloLectivoId, Integer diaSemana, Integer periodoOrden);

    Optional<WorkshopHorarioSlot> findByAula_IdAndCicloLectivo_IdAndDiaSemanaAndPeriodoOrdenAndIdNot(
            Long aulaId, Long cicloLectivoId, Integer diaSemana, Integer periodoOrden, Long excludeId);
}
