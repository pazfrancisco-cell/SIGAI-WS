package ar.edu.wellspring.sigaiws.modules.auth.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.security.PasswordPolicy;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.PasswordResetToken;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.PasswordResetTokenRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import ar.edu.wellspring.sigaiws.common.notification.EmailService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seccion 7.1: "Recuperacion de contraseña: envio de enlace temporal por email
 * (valido por 30 minutos, de un solo uso)."
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String MODULO = "AUTH";
    private final SecureRandom secureRandom = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final EmailService emailService;
    private final AuditService auditService;

    @Value("${sigai.seguridad.password-reset-token-minutos:30}")
    private int minutosValidez;

    @Value("${app.base-url:http://localhost:8443}")
    private String baseUrl;

    /**
     * Deliberadamente NO revela si el email existe o no (evita enumeracion de
     * usuarios): siempre se comporta igual desde la perspectiva del solicitante.
     */
    @Transactional
    public void solicitar(String email) {
        usuarioRepository.findByEmailIgnoreCase(email).ifPresent(usuario -> {
            String token = generarToken();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUsuario(usuario);
            resetToken.setToken(token);
            resetToken.setExpiraEn(LocalDateTime.now().plusMinutes(minutosValidez));
            tokenRepository.save(resetToken);

            String enlace = baseUrl + "/restablecer-password?token=" + token;
            emailService.enviarConPlantilla(usuario.getEmail(), "RECUPERACION_PASSWORD", Map.of(
                    "link_restablecer", enlace));

            auditService.registrar(MODULO, "SOLICITUD_RESET_PASSWORD", "Usuario", usuario.getId(), null, null);
        });
    }

    @Transactional
    public void restablecer(String token, String passwordNueva, String passwordConfirmacion) {
        if (!passwordNueva.equals(passwordConfirmacion)) {
            throw new BusinessException("Las contraseñas no coinciden.");
        }
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("El enlace de recuperacion no es valido."));
        if (!resetToken.esValido()) {
            throw new BusinessException("El enlace de recuperacion vencio o ya fue utilizado.");
        }
        passwordPolicy.validar(passwordNueva);

        Usuario usuario = resetToken.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(passwordNueva));
        usuario.setDebeCambiarPassword(false);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        resetToken.setUsado(true);
        tokenRepository.save(resetToken);

        auditService.registrar(MODULO, "RESET_PASSWORD_COMPLETADO", "Usuario", usuario.getId(), null, null);
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
