package ar.edu.wellspring.sigaiws.modules.mensajeria.repository;

import ar.edu.wellspring.sigaiws.modules.mensajeria.domain.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {
}
