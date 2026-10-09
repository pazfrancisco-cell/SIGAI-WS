package ar.edu.wellspring.sigaiws.modules.configuracion.web;

import ar.edu.wellspring.sigaiws.modules.configuracion.service.ParametroAcademicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Landing de Configuracion (IMPLEMENTACION.md 3.7): vista de solo lectura que
 * agrupa los accesos a las pantallas de configuracion en tarjetas, mostrando
 * el dato vigente de cada una. No agrega logica de negocio nueva.
 */
@Controller
@RequiredArgsConstructor
public class ConfiguracionIndexController {

    private final ParametroAcademicoService parametroAcademicoService;

    @GetMapping("/configuracion")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String index(Model model) {
        model.addAttribute("parametro", parametroAcademicoService.obtener());
        return "configuracion/index";
    }
}
