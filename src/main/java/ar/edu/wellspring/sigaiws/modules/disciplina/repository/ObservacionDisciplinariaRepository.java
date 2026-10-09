package ar.edu.wellspring.sigaiws.modules.disciplina.repository;

import ar.edu.wellspring.sigaiws.modules.disciplina.domain.ObservacionDisciplinaria;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObservacionDisciplinariaRepository extends JpaRepository<ObservacionDisciplinaria, Long> {
    List<ObservacionDisciplinaria> findAllByAlumno_IdAndCicloLectivo_IdOrderByFechaDesc(Long alumnoId, Long cicloLectivoId);
    long countByAlumno_IdAndCicloLectivo_Id(Long alumnoId, Long cicloLectivoId);
    List<ObservacionDisciplinaria> findAllByFechaGreaterThanEqual(LocalDate desde);
}
