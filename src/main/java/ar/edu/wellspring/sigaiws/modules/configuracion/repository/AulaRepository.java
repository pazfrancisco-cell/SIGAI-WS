package ar.edu.wellspring.sigaiws.modules.configuracion.repository;

import ar.edu.wellspring.sigaiws.modules.configuracion.domain.Aula;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AulaRepository extends JpaRepository<Aula, Long> {
    List<Aula> findAllByActivaTrueOrderByNombre();
    List<Aula> findAllByOrderByNombre();
    boolean existsByNombreIgnoreCase(String nombre);
}
