package ar.edu.wellspring.sigaiws.modules.configuracion.web;

import ar.edu.wellspring.sigaiws.modules.configuracion.dto.WorkshopForm;
import ar.edu.wellspring.sigaiws.modules.configuracion.service.WorkshopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/configuracion/workshops")
@PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
@RequiredArgsConstructor
public class WorkshopController {

    private final WorkshopService workshopService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("workshops", workshopService.listar());
        return "configuracion/workshops";
    }

    @GetMapping("/nuevo")
    public String nuevoForm(Model model) {
        model.addAttribute("workshopForm", new WorkshopForm());
        return "configuracion/workshop-form";
    }

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable Long id, Model model) {
        var workshop = workshopService.obtener(id);
        WorkshopForm form = new WorkshopForm();
        form.setId(workshop.getId());
        form.setNombre(workshop.getNombre());
        form.setDescripcion(workshop.getDescripcion());
        form.setBanda(workshop.getBanda());
        form.setActivo(workshop.isActivo());
        model.addAttribute("workshopForm", form);
        return "configuracion/workshop-form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute WorkshopForm form, BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "configuracion/workshop-form";
        }
        workshopService.guardar(form);
        redirectAttributes.addFlashAttribute("mensajeExito", "Workshop guardado correctamente.");
        return "redirect:/configuracion/workshops";
    }
}
