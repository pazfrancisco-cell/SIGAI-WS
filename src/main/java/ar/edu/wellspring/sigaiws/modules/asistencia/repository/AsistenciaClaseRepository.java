package ar.edu.wellspring.sigaiws.modules.asistencia.repository;

import ar.edu.wellspring.sigaiws.modules.asistencia.domain.AsistenciaClase;
import ar.edu.wellspring.sigaiws.modules.asistencia.domain.TipoAsistenciaClase;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsistenciaClaseRepository extends JpaRepository<AsistenciaClase, Long> {

    Optional<AsistenciaClase> findByAlumno_IdAndMateria_IdAndFecha(Long alumnoId, Long materiaId, LocalDate fecha);

    List<AsistenciaClase> findAllByAlumno_IdAndMateria_Id(Long alumnoId, Long materiaId);

    long countByAlumno_IdAndMateria_Id(Long alumnoId, Long materiaId);

    long countByAlumno_IdAndMateria_IdAndTipo(Long alumnoId, Long materiaId, TipoAsistenciaClase tipo);
}
