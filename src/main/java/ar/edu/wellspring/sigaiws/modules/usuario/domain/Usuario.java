package ar.edu.wellspring.sigaiws.modules.usuario.domain;

import ar.edu.wellspring.sigaiws.common.domain.BaseEntity;
import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Modulo 5.12. Entidad de dominio pura: no implementa UserDetails de Spring
 * Security (esa adaptacion vive en {@code modules.auth.security.SigaiUserPrincipal})
 * para no acoplar el modelo de datos al framework de seguridad.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "usuario")
public class Usuario extends BaseEntity {

    @Column(name = "nombre_completo", nullable = false, length = 160)
    private String nombreCompleto;

    @Column(name = "email", nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "debe_cambiar_password", nullable = false)
    private boolean debeCambiarPassword = true;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    /** Especialidades del docente (no aplican a otros roles): habilitan pantallas especificas de Educacion Fisica/Workshops. */
    @Column(name = "es_docente_educacion_fisica", nullable = false)
    private boolean esDocenteEducacionFisica = false;

    @Column(name = "es_docente_workshop", nullable = false)
    private boolean esDocenteWorkshop = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuario_rol",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<Rol> roles = new HashSet<>();

    public boolean tieneRol(RolNombre rolNombre) {
        return roles.stream().anyMatch(r -> r.getNombre() == rolNombre);
    }

    public boolean estaBloqueado() {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(LocalDateTime.now());
    }
}
