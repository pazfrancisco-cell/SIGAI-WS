package ar.edu.wellspring.sigaiws.modules.usuario.repository;

import ar.edu.wellspring.sigaiws.modules.usuario.domain.PasswordResetToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
}
