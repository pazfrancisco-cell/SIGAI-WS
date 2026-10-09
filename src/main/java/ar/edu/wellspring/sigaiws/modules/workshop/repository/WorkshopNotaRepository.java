package ar.edu.wellspring.sigaiws.modules.workshop.repository;

import ar.edu.wellspring.sigaiws.modules.workshop.domain.WorkshopNota;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkshopNotaRepository extends JpaRepository<WorkshopNota, Long> {
    Optional<WorkshopNota> findByEvaluacion_IdAndAlumno_Id(Long evaluacionId, Long alumnoId);
    List<WorkshopNota> findAllByEvaluacion_Id(Long evaluacionId);
}
