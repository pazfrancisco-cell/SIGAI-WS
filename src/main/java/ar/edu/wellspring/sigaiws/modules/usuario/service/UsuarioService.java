package ar.edu.wellspring.sigaiws.modules.usuario.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.common.exception.DuplicateResourceException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.common.notification.EmailService;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Rol;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import ar.edu.wellspring.sigaiws.modules.usuario.dto.UsuarioForm;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.RolRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Modulo 5.12 (Usuarios, Roles y Permisos). Todas las operaciones de escritura
 * quedan auditadas (seccion 3.5): alta, edicion, reseteo de password,
 * activacion/desactivacion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final String MODULO = "USUARIOS";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final RolCombinacionValidator rolCombinacionValidator;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;
    private final AuditService auditService;
    private final EmailService emailService;

    @Value("${sigai.superadmin.limite-recomendado:3}")
    private int limiteRecomendadoSuperadmin;

    @Transactional(readOnly = true)
    public Page<Usuario> buscar(String texto, boolean soloActivos, RolNombre rol, Pageable pageable) {
        return usuarioRepository.buscar(texto, soloActivos, rol, pageable);
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> NotFoundException.of("Usuario", id));
    }

    @Transactional
    public Usuario crear(UsuarioForm form) {
        if (usuarioRepository.existsByEmailIgnoreCase(form.getEmail())) {
            throw new DuplicateResourceException("email", "Ya existe un usuario con ese email.");
        }
        rolCombinacionValidator.validar(form.getRoles());
        advertirSiSuperaLimiteSuperadmin(form.getRoles());
        Set<Rol> roles = resolverRoles(form.getRoles());

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(form.getNombreCompleto());
        usuario.setEmail(form.getEmail().toLowerCase());
        usuario.setRoles(roles);
        usuario.setActivo(true);
        usuario.setDebeCambiarPassword(true);
        usuario.setEsDocenteEducacionFisica(form.isEsDocenteEducacionFisica());
        usuario.setEsDocenteWorkshop(form.isEsDocenteWorkshop());

        String passwordTemporal = passwordGenerator.generarTemporal();
        usuario.setPasswordHash(passwordEncoder.encode(passwordTemporal));

        usuario = usuarioRepository.save(usuario);
        auditService.registrar(MODULO, "CREAR", "Usuario", usuario.getId(), null, resumen(usuario));

        enviarCredencialesBienvenida(usuario, passwordTemporal);
        return usuario;
    }

    @Transactional
    public Usuario editar(Long id, UsuarioForm form) {
        Usuario usuario = obtener(id);
        String antes = resumen(usuario);

        if (!usuario.getEmail().equalsIgnoreCase(form.getEmail())
                && usuarioRepository.existsByEmailIgnoreCase(form.getEmail())) {
            throw new DuplicateResourceException("email", "Ya existe un usuario con ese email.");
        }

        rolCombinacionValidator.validar(form.getRoles());
        advertirSiSuperaLimiteSuperadmin(form.getRoles());

        usuario.setNombreCompleto(form.getNombreCompleto());
        usuario.setEmail(form.getEmail().toLowerCase());
        usuario.setRoles(resolverRoles(form.getRoles()));
        usuario.setEsDocenteEducacionFisica(form.isEsDocenteEducacionFisica());
        usuario.setEsDocenteWorkshop(form.isEsDocenteWorkshop());

        usuario = usuarioRepository.save(usuario);
        auditService.registrar(MODULO, "EDITAR", "Usuario", usuario.getId(), antes, resumen(usuario));
        return usuario;
    }

    @Transactional
    public void resetearPassword(Long id) {
        Usuario usuario = obtener(id);
        String passwordTemporal = passwordGenerator.generarTemporal();
        usuario.setPasswordHash(passwordEncoder.encode(passwordTemporal));
        usuario.setDebeCambiarPassword(true);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        auditService.registrar(MODULO, "RESETEAR_PASSWORD", "Usuario", usuario.getId(), null, null);
        enviarCredencialesBienvenida(usuario, passwordTemporal);
    }

    @Transactional
    public void activar(Long id) {
        cambiarEstado(id, true);
    }

    /**
     * "El usuario desactivado no puede iniciar sesion pero sus datos no se
     * eliminan" (seccion 5.12.4): baja logica, nunca fisica.
     */
    @Transactional
    public void desactivar(Long id) {
        cambiarEstado(id, false);
    }

    private void cambiarEstado(Long id, boolean activo) {
        Usuario usuario = obtener(id);
        boolean antes = usuario.isActivo();
        usuario.setActivo(activo);
        usuarioRepository.save(usuario);
        auditService.registrar(MODULO, activo ? "ACTIVAR" : "DESACTIVAR", "Usuario", usuario.getId(),
                "activo=" + antes, "activo=" + activo);
    }

    private Set<Rol> resolverRoles(Set<RolNombre> nombres) {
        return nombres.stream()
                .map(nombre -> rolRepository.findByNombre(nombre)
                        .orElseThrow(() -> new IllegalStateException("Rol no sembrado en BD: " + nombre)))
                .collect(Collectors.toSet());
    }

    private void advertirSiSuperaLimiteSuperadmin(Set<RolNombre> roles) {
        if (roles.contains(RolNombre.SUPERADMIN)) {
            long actuales = usuarioRepository.countByRoles_Nombre(RolNombre.SUPERADMIN);
            if (actuales >= limiteRecomendadoSuperadmin) {
                // Politica documental (seccion 5.12.2), no bloqueante: se registra
                // para que Auditoria/Superadmin lo revisen, pero no se impide el alta.
                log.warn("Se supero el limite recomendado de {} usuarios Superadmin (actuales: {})",
                        limiteRecomendadoSuperadmin, actuales);
            }
        }
    }

    private void enviarCredencialesBienvenida(Usuario usuario, String passwordTemporal) {
        emailService.enviarConPlantilla(usuario.getEmail(), "BIENVENIDA_USUARIO", Map.of(
                "nombre", usuario.getNombreCompleto(),
                "email", usuario.getEmail(),
                "password_temporal", passwordTemporal));
    }

    private String resumen(Usuario usuario) {
        String roles = usuario.getRoles().stream().map(r -> r.getNombre().name()).collect(Collectors.joining(","));
        return "nombre=%s, email=%s, roles=%s, activo=%s, esDocenteEducacionFisica=%s, esDocenteWorkshop=%s".formatted(
                usuario.getNombreCompleto(), usuario.getEmail(), roles, usuario.isActivo(),
                usuario.isEsDocenteEducacionFisica(), usuario.isEsDocenteWorkshop());
    }
}
