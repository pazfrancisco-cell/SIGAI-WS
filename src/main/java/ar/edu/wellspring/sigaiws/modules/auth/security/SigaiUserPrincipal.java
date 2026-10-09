package ar.edu.wellspring.sigaiws.modules.auth.security;

import ar.edu.wellspring.sigaiws.common.security.AuthenticatedActor;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adaptador entre la entidad de dominio Usuario y el contrato de Spring Security.
 * Cada rol se expone como autoridad "ROLE_&lt;NOMBRE&gt;" (convencion de
 * hasRole()/hasAnyRole() en las reglas de SecurityConfig y @PreAuthorize).
 */
public class SigaiUserPrincipal implements UserDetails, AuthenticatedActor {

    private final Usuario usuario;
    private final boolean tieneAsignacionMateria;

    public SigaiUserPrincipal(Usuario usuario) {
        this(usuario, false);
    }

    public SigaiUserPrincipal(Usuario usuario, boolean tieneAsignacionMateria) {
        this.usuario = usuario;
        this.tieneAsignacionMateria = tieneAsignacionMateria;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    /**
     * Seccion 5.5/5.6: un docente solo ve los accesos genericos de Asistencia/
     * Calificaciones/"Materias y workshops" si efectivamente tiene alguna
     * materia asignada en el ciclo activo (sino quedan vacios/obsoletos, ej.
     * un docente que solo dicta un workshop).
     */
    public boolean isTieneAsignacionMateria() {
        return tieneAsignacionMateria;
    }

    @Override
    public Long getId() {
        return usuario.getId();
    }

    public boolean debeCambiarPassword() {
        return usuario.isDebeCambiarPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = usuario.getRoles().stream()
                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol.getNombre().name()))
                .collect(Collectors.toSet());
        return authorities;
    }

    @Override
    public String getPassword() {
        return usuario.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return usuario.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !usuario.estaBloqueado();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return usuario.isActivo();
    }
}
