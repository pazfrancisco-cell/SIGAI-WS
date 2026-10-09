package ar.edu.wellspring.sigaiws.modules.workshop.repository;

import ar.edu.wellspring.sigaiws.modules.workshop.domain.WorkshopEvaluacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkshopEvaluacionRepository extends JpaRepository<WorkshopEvaluacion, Long> {
    List<WorkshopEvaluacion> findAllByWorkshop_IdAndCicloLectivo_IdAndCuatrimestreOrderByFecha(
            Long workshopId, Long cicloLectivoId, Integer cuatrimestre);
}
