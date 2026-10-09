package ar.edu.wellspring.sigaiws.modules.alumno.repository;

import ar.edu.wellspring.sigaiws.modules.alumno.domain.DocumentoAlumno;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoAlumnoRepository extends JpaRepository<DocumentoAlumno, Long> {
    List<DocumentoAlumno> findAllByAlumno_IdOrderBySubidoEnDesc(Long alumnoId);
}
