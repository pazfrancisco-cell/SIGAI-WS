package ar.edu.wellspring.sigaiws.modules.configuracion.web;

import ar.edu.wellspring.sigaiws.modules.configuracion.service.PlantillaEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/configuracion/plantillas-email")
@PreAuthorize("hasRole('SUPERADMIN')")
@RequiredArgsConstructor
public class PlantillaEmailController {

    private final PlantillaEmailService service;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("plantillas", service.listar());
        return "configuracion/plantillas-email";
    }

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable Long id, Model model) {
        model.addAttribute("plantilla", service.obtener(id));
        return "configuracion/plantilla-email-form";
    }

    @PostMapping("/{id}")
    public String guardar(@PathVariable Long id, @RequestParam String asunto, @RequestParam String cuerpo,
                           @RequestParam(defaultValue = "false") boolean habilitada,
                           RedirectAttributes redirectAttributes) {
        service.guardar(id, asunto, cuerpo, habilitada);
        redirectAttributes.addFlashAttribute("mensajeExito", "Plantilla guardada correctamente.");
        return "redirect:/configuracion/plantillas-email";
    }
}
