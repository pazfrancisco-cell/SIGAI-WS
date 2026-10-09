package ar.edu.wellspring.sigaiws.modules.edfisica.repository;

import ar.edu.wellspring.sigaiws.modules.edfisica.domain.PruebaFisicaDefinicion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PruebaFisicaDefinicionRepository extends JpaRepository<PruebaFisicaDefinicion, Long> {
    List<PruebaFisicaDefinicion> findAllByActivaTrueOrderByNombre();
    Optional<PruebaFisicaDefinicion> findByNombreIgnoreCase(String nombre);
}
