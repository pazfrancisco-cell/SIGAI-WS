package ar.edu.wellspring.sigaiws.modules.calendario.repository;

import ar.edu.wellspring.sigaiws.modules.calendario.domain.EventoAcademico;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventoAcademicoRepository extends JpaRepository<EventoAcademico, Long> {

    List<EventoAcademico> findAllByCicloLectivo_IdOrderByFechaInicio(Long cicloLectivoId);

    List<EventoAcademico> findAllByCicloLectivo_IdAndCursoDivisionIsNullOrderByFechaInicio(Long cicloLectivoId);

    @Query("SELECT e FROM EventoAcademico e WHERE e.cicloLectivo.id = :cicloLectivoId "
            + "AND e.cursoDivision.id IN :cursoDivisionIds ORDER BY e.fechaInicio")
    List<EventoAcademico> findAllByCicloYCursos(Long cicloLectivoId, List<Long> cursoDivisionIds);
}
