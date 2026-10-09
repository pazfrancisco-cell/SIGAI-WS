package ar.edu.wellspring.sigaiws.modules.asistencia.repository;

import ar.edu.wellspring.sigaiws.modules.asistencia.domain.JustificacionInasistencia;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JustificacionInasistenciaRepository extends JpaRepository<JustificacionInasistencia, Long> {
    Optional<JustificacionInasistencia> findByAsistenciaJornada_Id(Long asistenciaJornadaId);
}
