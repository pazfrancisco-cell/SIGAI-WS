package ar.edu.wellspring.sigaiws.modules.reporte.repository;

import ar.edu.wellspring.sigaiws.modules.reporte.domain.ReporteGenerado;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReporteGeneradoRepository extends JpaRepository<ReporteGenerado, Long> {
    List<ReporteGenerado> findAllByAlumno_IdOrderByGeneradoEnDesc(Long alumnoId);
}
