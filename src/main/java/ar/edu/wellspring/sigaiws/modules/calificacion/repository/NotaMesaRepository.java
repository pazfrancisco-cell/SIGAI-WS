package ar.edu.wellspring.sigaiws.modules.calificacion.repository;

import ar.edu.wellspring.sigaiws.modules.calificacion.domain.NotaMesa;
import ar.edu.wellspring.sigaiws.modules.calificacion.domain.TurnoMesa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotaMesaRepository extends JpaRepository<NotaMesa, Long> {

    Optional<NotaMesa> findByMateriaAdeudada_IdAndTurnoAndAnio(Long materiaAdeudadaId, TurnoMesa turno, Integer anio);

    List<NotaMesa> findAllByMateriaAdeudada_IdOrderByFechaAsc(Long materiaAdeudadaId);

    boolean existsByMateriaAdeudada_Id(Long materiaAdeudadaId);
}
