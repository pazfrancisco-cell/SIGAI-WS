package ar.edu.wellspring.sigaiws.web;

import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Seccion 4.4: enlace "perfil" del menu dropdown del avatar en la topbar. */
@Controller
public class PerfilController {

    @GetMapping("/perfil")
    public String perfil(@AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        model.addAttribute("usuario", principal.getUsuario());
        return "perfil";
    }
}
