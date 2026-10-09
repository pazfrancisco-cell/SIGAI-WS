package ar.edu.wellspring.sigaiws.modules.auth.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.security.PasswordPolicy;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cambio de contraseña por el propio usuario autenticado (primer ingreso o voluntario). */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final AuditService auditService;

    @Transactional
    public void cambiarPassword(Long usuarioId, String passwordActual, String passwordNueva, String confirmacion) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado."));

        if (!passwordEncoder.matches(passwordActual, usuario.getPasswordHash())) {
            throw new BusinessException("La contraseña actual no es correcta.");
        }
        if (!passwordNueva.equals(confirmacion)) {
            throw new BusinessException("Las contraseñas nuevas no coinciden.");
        }
        passwordPolicy.validar(passwordNueva);

        usuario.setPasswordHash(passwordEncoder.encode(passwordNueva));
        usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);

        auditService.registrar("AUTH", "CAMBIO_PASSWORD", "Usuario", usuario.getId(), null, null);
    }
}
