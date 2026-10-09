package ar.edu.wellspring.sigaiws.web;

import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.modules.alerta.service.AlertaService;
import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.mensajeria.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Seccion 4.4: badges numericos del topbar, expuestos como atributos de modelo
 * globales porque el fragmento de topbar se incluye en todas las vistas autenticadas.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final AlertaService alertaService;
    private final NotificacionService notificacionService;

    /** Alertas institucionales: solo para los roles que acceden al Centro de Alertas. */
    @ModelAttribute("alertasActivasCount")
    public Integer alertasActivasCount(@AuthenticationPrincipal SigaiUserPrincipal principal) {
        if (principal == null) {
            return null;
        }
        var usuario = principal.getUsuario();
        boolean gestion = usuario.tieneRol(RolNombre.SUPERADMIN) || usuario.tieneRol(RolNombre.DIRECTIVO)
                || usuario.tieneRol(RolNombre.ADMINISTRATIVO) || usuario.tieneRol(RolNombre.PRECEPTOR);
        return gestion ? alertaService.activas().size() : null;
    }

    /** Notificaciones personales sin leer (campanita, todos los roles). */
    @ModelAttribute("notificacionesNoLeidasCount")
    public Long notificacionesNoLeidasCount(@AuthenticationPrincipal SigaiUserPrincipal principal) {
        return principal == null ? null : notificacionService.noLeidas(principal.getId());
    }
}
