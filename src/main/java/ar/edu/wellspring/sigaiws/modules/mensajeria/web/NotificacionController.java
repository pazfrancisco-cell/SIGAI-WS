package ar.edu.wellspring.sigaiws.modules.mensajeria.web;

import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.mensajeria.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Seccion 5.10.2: panel de notificaciones personales (campanita del topbar). Acceso: todos los roles. */
@Controller
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public String listar(@AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        model.addAttribute("notificaciones", notificacionService.deUsuario(principal.getId()));
        return "notificaciones/lista";
    }

    /** Marca la notificacion como leida y lleva a lo que la genero. */
    @GetMapping("/{id}")
    public String abrir(@PathVariable Long id, @AuthenticationPrincipal SigaiUserPrincipal principal) {
        String enlace = notificacionService.abrir(id, principal.getId());
        // Solo rutas internas (las genera el propio sistema; se descarta cualquier otra cosa por las dudas).
        boolean esInterno = enlace != null && enlace.startsWith("/") && !enlace.startsWith("//");
        return "redirect:" + (esInterno ? enlace : "/notificaciones");
    }

    @PostMapping("/marcar-todas")
    public String marcarTodas(@AuthenticationPrincipal SigaiUserPrincipal principal) {
        notificacionService.marcarTodasLeidas(principal.getId());
        return "redirect:/notificaciones";
    }
}
