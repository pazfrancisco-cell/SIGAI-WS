package ar.edu.wellspring.sigaiws.modules.usuario.service;

import static ar.edu.wellspring.sigaiws.common.enums.RolNombre.ADMINISTRATIVO;
import static ar.edu.wellspring.sigaiws.common.enums.RolNombre.DIRECTIVO;
import static ar.edu.wellspring.sigaiws.common.enums.RolNombre.PRECEPTOR;

import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Regla de negocio de la seccion 5.12.3: "El sistema permite que un usuario
 * tenga mas de un rol activo. Las combinaciones validas son: Preceptor +
 * Administrativo, Administrativo + Preceptor, Directivo + Administrativo,
 * Directivo + Preceptor. Un docente no puede combinarse con otros roles
 * operativos por diseño, ya que sus permisos estan acotados a sus propias
 * asignaciones."
 *
 * Interpretacion (sin contradecir el texto): Superadmin, Docente y Padre/Tutor
 * son roles exclusivos (no combinables con ningun otro, incluido entre si).
 * Directivo, Administrativo y Preceptor solo pueden combinarse de a pares,
 * exactamente en las combinaciones listadas; no se documenta una combinacion
 * valida de los tres a la vez, por lo que se rechaza.
 */
@Component
public class RolCombinacionValidator {

    private static final Set<RolNombre> ROLES_EXCLUSIVOS =
            Set.of(RolNombre.SUPERADMIN, RolNombre.DOCENTE, RolNombre.PADRE_TUTOR);

    private static final Set<Set<RolNombre>> COMBINACIONES_VALIDAS = Set.of(
            Set.of(PRECEPTOR, ADMINISTRATIVO),
            Set.of(DIRECTIVO, ADMINISTRATIVO),
            Set.of(DIRECTIVO, PRECEPTOR)
    );

    public void validar(Set<RolNombre> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new BusinessException("El usuario debe tener al menos un rol asignado.");
        }
        if (roles.size() == 1) {
            return; // un unico rol siempre es valido
        }
        boolean incluyeExclusivo = roles.stream().anyMatch(ROLES_EXCLUSIVOS::contains);
        if (incluyeExclusivo) {
            throw new BusinessException(
                    "Los roles Superadministrador, Docente y Padre/Tutor no pueden combinarse con otros roles.");
        }
        if (!COMBINACIONES_VALIDAS.contains(roles)) {
            throw new BusinessException(
                    "Combinacion de roles no permitida. Combinaciones validas: Preceptor+Administrativo, "
                            + "Directivo+Administrativo, Directivo+Preceptor.");
        }
    }
}
