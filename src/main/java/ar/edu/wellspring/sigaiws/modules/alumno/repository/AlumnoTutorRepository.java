package ar.edu.wellspring.sigaiws.modules.alumno.repository;

import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoTutor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlumnoTutorRepository extends JpaRepository<AlumnoTutor, Long> {
    List<AlumnoTutor> findAllByAlumno_Id(Long alumnoId);
    long countByAlumno_Id(Long alumnoId);
    List<AlumnoTutor> findAllByTutor_UsuarioId(Long usuarioId);
}
