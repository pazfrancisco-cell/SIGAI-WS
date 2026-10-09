package ar.edu.wellspring.sigaiws.modules.mensajeria.repository;

import ar.edu.wellspring.sigaiws.modules.mensajeria.domain.Mensaje;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {
    List<Mensaje> findAllByConversacion_IdOrderByEnviadoEn(Long conversacionId);
    Mensaje findTopByConversacion_IdOrderByEnviadoEnDesc(Long conversacionId);
}
