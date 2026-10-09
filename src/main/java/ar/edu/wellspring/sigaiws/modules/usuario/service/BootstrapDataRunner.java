package ar.edu.wellspring.sigaiws.modules.usuario.service;

import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Rol;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.RolRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el primer usuario Superadministrador en el arranque, si todavia no existe
 * ninguno. No se hace via Flyway (V13) para evitar un password hash hardcodeado
 * en el repositorio (seccion 7.1: "no hay registro libre; todos los usuarios son
 * creados exclusivamente por el Superadministrador" - este es el unico caso
 * legitimo de "auto-creacion", limitado al arranque inicial del sistema).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapDataRunner implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${SIGAI_BOOTSTRAP_ADMIN_EMAIL:}")
    private String bootstrapEmail;

    @Value("${SIGAI_BOOTSTRAP_ADMIN_PASSWORD:}")
    private String bootstrapPassword;

    @Override
    public void run(String... args) {
        boolean existeSuperadmin = usuarioRepository.countByRoles_Nombre(RolNombre.SUPERADMIN) > 0;
        if (existeSuperadmin) {
            return;
        }
        if (bootstrapEmail.isBlank() || bootstrapPassword.isBlank()) {
            log.warn("No existe ningun usuario Superadministrador y no se definieron "
                    + "SIGAI_BOOTSTRAP_ADMIN_EMAIL / SIGAI_BOOTSTRAP_ADMIN_PASSWORD. "
                    + "El sistema arrancara sin forma de iniciar sesion hasta crear uno.");
            return;
        }

        Rol rolSuperadmin = rolRepository.findByNombre(RolNombre.SUPERADMIN)
                .orElseThrow(() -> new IllegalStateException("Rol SUPERADMIN no sembrado (V13 no se aplico)"));

        Usuario admin = new Usuario();
        admin.setNombreCompleto("Superadministrador Inicial");
        admin.setEmail(bootstrapEmail.toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        admin.setDebeCambiarPassword(true);
        admin.setActivo(true);
        admin.setRoles(Set.of(rolSuperadmin));
        usuarioRepository.save(admin);

        log.info("Usuario Superadministrador inicial creado ({}). Debera cambiar la contraseña al ingresar.",
                bootstrapEmail);
    }
}
