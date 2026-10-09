package ar.edu.wellspring.sigaiws.modules.disciplina.repository;

import ar.edu.wellspring.sigaiws.modules.disciplina.domain.FirmaDisciplinaria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FirmaDisciplinariaRepository extends JpaRepository<FirmaDisciplinaria, Long> {
    List<FirmaDisciplinaria> findAllByAlumno_IdAndCicloLectivo_IdOrderByNumero(Long alumnoId, Long cicloLectivoId);
    long countByAlumno_IdAndCicloLectivo_Id(Long alumnoId, Long cicloLectivoId);
}
