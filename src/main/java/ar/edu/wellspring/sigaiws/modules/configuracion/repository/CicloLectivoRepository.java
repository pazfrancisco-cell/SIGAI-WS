package ar.edu.wellspring.sigaiws.modules.configuracion.repository;

import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CicloLectivoRepository extends JpaRepository<CicloLectivo, Long> {
    Optional<CicloLectivo> findByActivoTrue();
    boolean existsByAnio(Integer anio);
}
