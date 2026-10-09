package ar.edu.wellspring.sigaiws.modules.edfisica.repository;

import ar.edu.wellspring.sigaiws.modules.edfisica.domain.AsistenciaEducacionFisica;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsistenciaEducacionFisicaRepository extends JpaRepository<AsistenciaEducacionFisica, Long> {
    Optional<AsistenciaEducacionFisica> findByAlumno_IdAndGrupo_IdAndFecha(Long alumnoId, Long grupoId, LocalDate fecha);
    List<AsistenciaEducacionFisica> findAllByGrupo_IdAndFecha(Long grupoId, LocalDate fecha);
}
