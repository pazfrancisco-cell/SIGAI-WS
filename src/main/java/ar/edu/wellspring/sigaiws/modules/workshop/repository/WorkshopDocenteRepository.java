package ar.edu.wellspring.sigaiws.modules.workshop.repository;

import ar.edu.wellspring.sigaiws.modules.workshop.domain.WorkshopDocente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkshopDocenteRepository extends JpaRepository<WorkshopDocente, Long> {
    List<WorkshopDocente> findAllByWorkshop_Id(Long workshopId);
    Optional<WorkshopDocente> findByDocente_Id(Long docenteId);
    boolean existsByDocente_Id(Long docenteId);
}
