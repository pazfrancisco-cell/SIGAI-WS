package ar.edu.wellspring.sigaiws.modules.configuracion.web;

import ar.edu.wellspring.sigaiws.modules.configuracion.dto.PeriodoHorarioForm;
import ar.edu.wellspring.sigaiws.modules.configuracion.service.PeriodoHorarioService;
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

/** Seccion 5.14.2 ("Horarios y Estructura del Dia"): reservado al Superadmin. */
@Controller
@RequestMapping("/configuracion/periodos-horarios")
@PreAuthorize("hasRole('SUPERADMIN')")
@RequiredArgsConstructor
public class PeriodoHorarioController {

    private final PeriodoHorarioService periodoHorarioService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("periodos", periodoHorarioService.listar());
        return "configuracion/periodos-horarios";
    }

    @GetMapping("/nuevo")
    public String nuevoForm(Model model) {
        model.addAttribute("periodoHorarioForm", new PeriodoHorarioForm());
        return "configuracion/periodo-horario-form";
    }

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable Long id, Model model) {
        var periodo = periodoHorarioService.obtener(id);
        PeriodoHorarioForm form = new PeriodoHorarioForm();
        form.setId(periodo.getId());
        form.setOrden(periodo.getOrden());
        form.setNombre(periodo.getNombre());
        form.setHoraInicio(periodo.getHoraInicio());
        form.setHoraFin(periodo.getHoraFin());
        form.setTipo(periodo.getTipo());
        model.addAttribute("periodoHorarioForm", form);
        return "configuracion/periodo-horario-form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute PeriodoHorarioForm form, BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "configuracion/periodo-horario-form";
        }
        periodoHorarioService.guardar(form);
        redirectAttributes.addFlashAttribute("mensajeExito", "Periodo guardado correctamente.");
        return "redirect:/configuracion/periodos-horarios";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        periodoHorarioService.eliminar(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Periodo eliminado.");
        return "redirect:/configuracion/periodos-horarios";
    }
}
