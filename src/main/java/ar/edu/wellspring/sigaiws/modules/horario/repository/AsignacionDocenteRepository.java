package ar.edu.wellspring.sigaiws.modules.horario.repository;

import ar.edu.wellspring.sigaiws.modules.horario.domain.AsignacionDocente;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsignacionDocenteRepository extends JpaRepository<AsignacionDocente, Long> {

    List<AsignacionDocente> findAllByDocente_IdAndCicloLectivo_Id(Long docenteId, Long cicloLectivoId);

    boolean existsByDocente_IdAndCicloLectivo_Id(Long docenteId, Long cicloLectivoId);

    List<AsignacionDocente> findAllByCicloLectivo_IdOrderByDocente_NombreCompleto(Long cicloLectivoId);

    boolean existsByDocente_IdAndMateria_IdAndCursoDivision_IdAndCicloLectivo_Id(
            Long docenteId, Long materiaId, Long cursoDivisionId, Long cicloLectivoId);

    List<AsignacionDocente> findAllByMateria_IdAndCicloLectivo_Id(Long materiaId, Long cicloLectivoId);
}
