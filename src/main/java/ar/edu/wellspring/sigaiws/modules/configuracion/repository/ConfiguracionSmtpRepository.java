package ar.edu.wellspring.sigaiws.modules.configuracion.repository;

import ar.edu.wellspring.sigaiws.modules.configuracion.domain.ConfiguracionSmtp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionSmtpRepository extends JpaRepository<ConfiguracionSmtp, Long> {
}
