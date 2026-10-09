package ar.edu.wellspring.sigaiws.modules.configuracion.repository;

import ar.edu.wellspring.sigaiws.modules.configuracion.domain.PeriodoHorario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodoHorarioRepository extends JpaRepository<PeriodoHorario, Long> {
    List<PeriodoHorario> findAllByOrderByOrden();
    List<PeriodoHorario> findAllByTipoOrderByOrden(PeriodoHorario.Tipo tipo);
}
