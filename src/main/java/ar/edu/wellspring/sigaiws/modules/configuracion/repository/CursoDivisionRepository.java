package ar.edu.wellspring.sigaiws.modules.configuracion.repository;

import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CursoDivision;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CursoDivisionRepository extends JpaRepository<CursoDivision, Long> {

    /** Solo cursos activos: es la lista que usan todos los selectores del sistema para cargas nuevas. */
    @Query("select c from CursoDivision c where c.activa = true order by c.anio asc, c.division asc")
    List<CursoDivision> findAllByOrderByAnioAscDivisionAsc();

    /** Activos e inactivos, para la pantalla de gestion de cursos. */
    @Query("select c from CursoDivision c order by c.anio asc, c.division asc")
    List<CursoDivision> todosOrdenados();

    Optional<CursoDivision> findByAnioAndDivision(Integer anio, String division);

    List<CursoDivision> findAllByAnioAndActivaTrueOrderByDivision(Integer anio);
}
