package ar.edu.wellspring.sigaiws.modules.configuracion.repository;

import ar.edu.wellspring.sigaiws.modules.configuracion.domain.Materia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MateriaRepository extends JpaRepository<Materia, Long> {
    List<Materia> findAllByActivaTrueOrderByNombre();
    List<Materia> findAllByOrderByNombre();
}
