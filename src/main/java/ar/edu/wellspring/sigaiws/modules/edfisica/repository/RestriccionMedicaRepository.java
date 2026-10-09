package ar.edu.wellspring.sigaiws.modules.edfisica.repository;

import ar.edu.wellspring.sigaiws.modules.edfisica.domain.RestriccionMedica;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestriccionMedicaRepository extends JpaRepository<RestriccionMedica, Long> {
    List<RestriccionMedica> findAllByAlumno_Id(Long alumnoId);
    List<RestriccionMedica> findAllByOrderByFechaInicioDesc();
}
