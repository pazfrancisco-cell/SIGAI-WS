package ar.edu.wellspring.sigaiws.modules.disciplina.repository;

import ar.edu.wellspring.sigaiws.modules.disciplina.domain.ActaDisciplinaria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActaDisciplinariaRepository extends JpaRepository<ActaDisciplinaria, Long> {
    List<ActaDisciplinaria> findAllByAlumno_IdAndCicloLectivo_Id(Long alumnoId, Long cicloLectivoId);
}
