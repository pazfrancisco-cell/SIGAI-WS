package ar.edu.wellspring.sigaiws.modules.alumno.repository;

import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoCicloLectivo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlumnoCicloLectivoRepository extends JpaRepository<AlumnoCicloLectivo, Long> {

    Optional<AlumnoCicloLectivo> findByAlumno_IdAndCicloLectivo_Id(Long alumnoId, Long cicloLectivoId);

    List<AlumnoCicloLectivo> findAllByAlumno_IdOrderByCicloLectivo_AnioDesc(Long alumnoId);

    List<AlumnoCicloLectivo> findAllByCicloLectivo_Id(Long cicloLectivoId);

    List<AlumnoCicloLectivo> findAllByCursoDivision_IdAndCicloLectivo_IdOrderByAlumno_ApellidoAscAlumno_NombreAsc(
            Long cursoDivisionId, Long cicloLectivoId);

    List<AlumnoCicloLectivo> findAllByWorkshopC1_IdAndCicloLectivo_Id(Long workshopId, Long cicloLectivoId);

    List<AlumnoCicloLectivo> findAllByWorkshopC2_IdAndCicloLectivo_Id(Long workshopId, Long cicloLectivoId);

    List<AlumnoCicloLectivo> findAllByCicloLectivo_IdAndWorkshopC2IsNull(Long cicloLectivoId);

    List<AlumnoCicloLectivo> findAllByCicloLectivo_IdAndCursoDivision_IdAndWorkshopC2IsNullOrderByAlumno_ApellidoAscAlumno_NombreAsc(
            Long cicloLectivoId, Long cursoDivisionId);

    List<AlumnoCicloLectivo> findAllByCicloLectivo_IdAndWorkshopC1IsNull(Long cicloLectivoId);

    List<AlumnoCicloLectivo> findAllByCicloLectivo_IdAndCursoDivision_IdAndWorkshopC1IsNullOrderByAlumno_ApellidoAscAlumno_NombreAsc(
            Long cicloLectivoId, Long cursoDivisionId);
}
