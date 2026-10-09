package ar.edu.wellspring.sigaiws.modules.calificacion.repository;

import ar.edu.wellspring.sigaiws.modules.calificacion.domain.Nota;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotaRepository extends JpaRepository<Nota, Long> {
    Optional<Nota> findByEvaluacion_IdAndAlumno_Id(Long evaluacionId, Long alumnoId);
    List<Nota> findAllByEvaluacion_Id(Long evaluacionId);
    List<Nota> findAllByAlumno_IdAndEvaluacion_Materia_IdAndEvaluacion_CicloLectivo_IdOrderByEvaluacion_Fecha(
            Long alumnoId, Long materiaId, Long cicloLectivoId);
}
