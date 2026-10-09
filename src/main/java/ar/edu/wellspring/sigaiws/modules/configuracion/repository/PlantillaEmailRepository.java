package ar.edu.wellspring.sigaiws.modules.configuracion.repository;

import ar.edu.wellspring.sigaiws.modules.configuracion.domain.PlantillaEmail;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantillaEmailRepository extends JpaRepository<PlantillaEmail, Long> {

    Optional<PlantillaEmail> findByTipo(String tipo);

    List<PlantillaEmail> findAllByOrderByDescripcion();
}
