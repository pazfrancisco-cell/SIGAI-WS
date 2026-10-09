package ar.edu.wellspring.sigaiws.modules.calificacion.repository;

import ar.edu.wellspring.sigaiws.modules.calificacion.domain.Evaluacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {
    List<Evaluacion> findAllByMateria_IdAndCursoDivision_IdAndCicloLectivo_IdAndCuatrimestreOrderByFecha(
            Long materiaId, Long cursoDivisionId, Long cicloLectivoId, Integer cuatrimestre);

    List<Evaluacion> findAllByCursoDivision_IdAndCicloLectivo_Id(Long cursoDivisionId, Long cicloLectivoId);

    List<Evaluacion> findAllByCreadoPor_IdAndCicloLectivo_IdAndCuatrimestre(Long creadoPorId, Long cicloLectivoId, Integer cuatrimestre);
}
