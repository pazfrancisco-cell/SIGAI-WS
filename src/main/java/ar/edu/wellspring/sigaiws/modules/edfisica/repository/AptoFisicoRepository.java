package ar.edu.wellspring.sigaiws.modules.edfisica.repository;

import ar.edu.wellspring.sigaiws.modules.edfisica.domain.AptoFisico;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AptoFisicoRepository extends JpaRepository<AptoFisico, Long> {
    Optional<AptoFisico> findByAlumno_IdAndCicloLectivo_Id(Long alumnoId, Long cicloLectivoId);
    boolean existsByAlumno_IdAndCicloLectivo_Id(Long alumnoId, Long cicloLectivoId);
}
