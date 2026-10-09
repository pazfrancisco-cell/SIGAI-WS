package ar.edu.wellspring.sigaiws.modules.auth.security;

import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.horario.repository.AsignacionDocenteRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final CicloLectivoRepository cicloLectivoRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales invalidas"));
        boolean tieneAsignacionMateria = usuario.tieneRol(RolNombre.DOCENTE)
                && cicloLectivoRepository.findByActivoTrue()
                        .map(ciclo -> asignacionDocenteRepository.existsByDocente_IdAndCicloLectivo_Id(usuario.getId(), ciclo.getId()))
                        .orElse(false);
        return new SigaiUserPrincipal(usuario, tieneAsignacionMateria);
    }
}
