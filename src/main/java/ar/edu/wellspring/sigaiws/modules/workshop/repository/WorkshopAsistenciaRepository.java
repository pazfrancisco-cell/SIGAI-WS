package ar.edu.wellspring.sigaiws.modules.workshop.repository;

import ar.edu.wellspring.sigaiws.modules.workshop.domain.WorkshopAsistencia;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkshopAsistenciaRepository extends JpaRepository<WorkshopAsistencia, Long> {
    Optional<WorkshopAsistencia> findByAlumno_IdAndWorkshop_IdAndFecha(Long alumnoId, Long workshopId, LocalDate fecha);
}
