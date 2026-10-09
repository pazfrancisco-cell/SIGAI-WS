package ar.edu.wellspring.sigaiws.modules.usuario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.common.exception.DuplicateResourceException;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.notification.EmailService;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Rol;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import ar.edu.wellspring.sigaiws.modules.usuario.dto.UsuarioForm;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.RolRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** Seccion 5.12.4 (Gestion de Usuarios). */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private AuditService auditService;
    @Mock private EmailService emailService;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(
                usuarioRepository, rolRepository,
                new RolCombinacionValidator(),
                new BCryptPasswordEncoder(4), // factor bajo solo para que el test corra rapido
                new PasswordGenerator(),
                auditService,
                emailService
        );
        ReflectionTestUtils.setField(usuarioService, "limiteRecomendadoSuperadmin", 3);
    }

    @Test
    void noPermiteCrearUsuarioConEmailDuplicado() {
        UsuarioForm form = formularioValido();
        when(usuarioRepository.existsByEmailIgnoreCase(form.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(form))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void rechazaCombinacionDeRolesInvalida() {
        UsuarioForm form = formularioValido();
        form.setRoles(Set.of(RolNombre.DOCENTE, RolNombre.PRECEPTOR)); // Docente no combina (seccion 5.12.3)
        lenient().when(usuarioRepository.existsByEmailIgnoreCase(form.getEmail())).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.crear(form)).isInstanceOf(BusinessException.class);
    }

    @Test
    void alCrearUsuarioQuedaMarcadoParaCambiarPasswordEnElPrimerIngreso() {
        UsuarioForm form = formularioValido();
        when(usuarioRepository.existsByEmailIgnoreCase(form.getEmail())).thenReturn(false);
        when(rolRepository.findByNombre(RolNombre.PRECEPTOR)).thenReturn(Optional.of(rol(RolNombre.PRECEPTOR)));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario creado = usuarioService.crear(form);

        assertThat(creado.isDebeCambiarPassword()).isTrue();
        assertThat(creado.isActivo()).isTrue();
        assertThat(creado.getPasswordHash()).isNotBlank();
    }

    private UsuarioForm formularioValido() {
        UsuarioForm form = new UsuarioForm();
        form.setNombreCompleto("Maria Preceptora");
        form.setEmail("maria.preceptora@wellspringschool.edu.ar");
        form.setRoles(Set.of(RolNombre.PRECEPTOR));
        form.setActivo(true);
        return form;
    }

    private Rol rol(RolNombre nombre) {
        return new Rol(1L, nombre, nombre.getEtiqueta());
    }
}
