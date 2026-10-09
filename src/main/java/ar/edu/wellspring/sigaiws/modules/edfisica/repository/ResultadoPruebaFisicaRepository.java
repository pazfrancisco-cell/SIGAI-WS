package ar.edu.wellspring.sigaiws.modules.edfisica.repository;

import ar.edu.wellspring.sigaiws.modules.edfisica.domain.ResultadoPruebaFisica;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultadoPruebaFisicaRepository extends JpaRepository<ResultadoPruebaFisica, Long> {
    List<ResultadoPruebaFisica> findAllByPrueba_Id(Long pruebaId);
    Optional<ResultadoPruebaFisica> findByPrueba_IdAndAlumno_Id(Long pruebaId, Long alumnoId);

    /** Historial completo de un alumno, en cualquier grupo/ciclo lectivo, para armar la evolucion por prueba (seccion 5.7.4). */
    List<ResultadoPruebaFisica> findAllByAlumno_IdOrderByPrueba_FechaAsc(Long alumnoId);

    void deleteAllByPrueba_Id(Long pruebaId);
}
