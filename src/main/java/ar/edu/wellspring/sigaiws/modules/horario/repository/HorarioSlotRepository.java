package ar.edu.wellspring.sigaiws.modules.horario.repository;

import ar.edu.wellspring.sigaiws.modules.horario.domain.HorarioSlot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioSlotRepository extends JpaRepository<HorarioSlot, Long> {

    List<HorarioSlot> findAllByCursoDivision_IdAndCicloLectivo_Id(Long cursoDivisionId, Long cicloLectivoId);

    List<HorarioSlot> findAllByDocente_IdAndCicloLectivo_Id(Long docenteId, Long cicloLectivoId);

    Optional<HorarioSlot> findByCursoDivision_IdAndCicloLectivo_IdAndDiaSemanaAndPeriodoOrden(
            Long cursoDivisionId, Long cicloLectivoId, Integer diaSemana, Integer periodoOrden);

    Optional<HorarioSlot> findByDocente_IdAndCicloLectivo_IdAndDiaSemanaAndPeriodoOrdenAndIdNot(
            Long docenteId, Long cicloLectivoId, Integer diaSemana, Integer periodoOrden, Long excludeId);

    Optional<HorarioSlot> findByAula_IdAndCicloLectivo_IdAndDiaSemanaAndPeriodoOrdenAndIdNot(
            Long aulaId, Long cicloLectivoId, Integer diaSemana, Integer periodoOrden, Long excludeId);

    List<HorarioSlot> findAllByCicloLectivo_Id(Long cicloLectivoId);

    boolean existsByAula_Id(Long aulaId);

    boolean existsByAula_IdAndCicloLectivo_IdAndDiaSemanaAndPeriodoOrden(
            Long aulaId, Long cicloLectivoId, Integer diaSemana, Integer periodoOrden);
}
