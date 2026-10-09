package ar.edu.wellspring.sigaiws.modules.mensajeria.repository;

import ar.edu.wellspring.sigaiws.modules.mensajeria.domain.ComunicadoInstitucional;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComunicadoInstitucionalRepository extends JpaRepository<ComunicadoInstitucional, Long> {
    List<ComunicadoInstitucional> findAllByOrderByCreadoEnDesc();

    List<ComunicadoInstitucional> findAllByIdInOrderByCreadoEnDesc(Collection<Long> ids);
}
