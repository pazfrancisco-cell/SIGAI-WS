package ar.edu.wellspring.sigaiws.modules.asistencia.repository;

import ar.edu.wellspring.sigaiws.modules.asistencia.domain.AsistenciaJornada;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsistenciaJornadaRepository extends JpaRepository<AsistenciaJornada, Long> {

    Optional<AsistenciaJornada> findByAlumno_IdAndFechaAndTurno(Long alumnoId, LocalDate fecha, AsistenciaJornada.Turno turno);

    List<AsistenciaJornada> findAllByAlumno_IdInAndFechaAndTurno(List<Long> alumnoIds, LocalDate fecha, AsistenciaJornada.Turno turno);

    List<AsistenciaJornada> findAllByAlumno_IdInAndFechaBetween(List<Long> alumnoIds, LocalDate desde, LocalDate hasta);

    List<AsistenciaJornada> findAllByAlumno_IdAndFechaBetweenOrderByFecha(Long alumnoId, LocalDate desde, LocalDate hasta);

    List<AsistenciaJornada> findAllByAlumno_IdOrderByFechaDesc(Long alumnoId);

    long countByFechaBetween(LocalDate desde, LocalDate hasta);

    long countByFechaBetweenAndTipo(LocalDate desde, LocalDate hasta, ar.edu.wellspring.sigaiws.modules.asistencia.domain.TipoAsistenciaJornada tipo);
}
