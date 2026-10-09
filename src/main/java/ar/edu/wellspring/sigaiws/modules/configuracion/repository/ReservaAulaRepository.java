package ar.edu.wellspring.sigaiws.modules.configuracion.repository;

import ar.edu.wellspring.sigaiws.modules.configuracion.domain.ReservaAula;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaAulaRepository extends JpaRepository<ReservaAula, Long> {
    Optional<ReservaAula> findByAula_IdAndFechaAndPeriodoOrden(Long aulaId, LocalDate fecha, Integer periodoOrden);

    List<ReservaAula> findAllByDocente_IdOrderByFechaDesc(Long docenteId);
}
