package ar.edu.wellspring.sigaiws.modules.auth.security;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seccion 7.1: "Limite de intentos fallidos: 5 intentos consecutivos incorrectos
 * bloquean la cuenta por 15 minutos." Escucha los eventos que Spring Security
 * publica en cada intento de autenticacion (exito o fallo) para actualizar los
 * contadores de Usuario y dejar registro en auditoria.
 */
@Component
@RequiredArgsConstructor
public class LoginAttemptListener {

    private static final String MODULO = "AUTH";

    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;

    @Value("${sigai.seguridad.intentos-fallidos-max:5}")
    private int intentosMaximos;

    @Value("${sigai.seguridad.bloqueo-minutos:15}")
    private int minutosBloqueo;

    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();
        usuarioRepository.findByEmailIgnoreCase(email).ifPresent(usuario -> {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioRepository.save(usuario);
            auditService.registrar(MODULO, "LOGIN_EXITOSO", "Usuario", usuario.getId(), null, null);
        });
    }

    @EventListener
    @Transactional
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String email = event.getAuthentication().getName();
        usuarioRepository.findByEmailIgnoreCase(email).ifPresent(usuario -> {
            // Si ya esta bloqueada, no se sigue incrementando (evita extender el bloqueo indefinidamente
            // ante reintentos automatizados); el bloqueo vence naturalmente a los N minutos.
            if (usuario.estaBloqueado()) {
                return;
            }
            usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
            if (usuario.getIntentosFallidos() >= intentosMaximos) {
                usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(minutosBloqueo));
            }
            usuarioRepository.save(usuario);
            auditService.registrar(MODULO, "LOGIN_FALLIDO", "Usuario", usuario.getId(),
                    null, "intentosFallidos=" + usuario.getIntentosFallidos());
        });
        if (usuarioRepository.findByEmailIgnoreCase(email).isEmpty()) {
            // Email no registrado: se audita sin usuario para detectar intentos de fuerza bruta
            // sin filtrar si la cuenta existe (mismo mensaje generico al usuario).
            auditService.registrarAnonimo(MODULO, "LOGIN_FALLIDO_EMAIL_DESCONOCIDO", email);
        }
    }
}
