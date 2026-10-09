package ar.edu.wellspring.sigaiws.modules.alumno.repository;

import ar.edu.wellspring.sigaiws.modules.alumno.domain.Tutor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TutorRepository extends JpaRepository<Tutor, Long> {
    Optional<Tutor> findByDniHash(String dniHash);
    boolean existsByEmailIgnoreCase(String email);
}
