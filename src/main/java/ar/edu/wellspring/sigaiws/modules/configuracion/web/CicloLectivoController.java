package ar.edu.wellspring.sigaiws.modules.configuracion.web;

import ar.edu.wellspring.sigaiws.modules.configuracion.dto.CicloLectivoForm;
import ar.edu.wellspring.sigaiws.modules.configuracion.service.CicloLectivoService;
import ar.edu.wellspring.sigaiws.modules.configuracion.service.CierreCuatrimestreService;
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

/**
 * Seccion 5.12.2: la gestion de ciclos lectivos esta reservada al Superadmin
 * ("gestion de ciclos lectivos y parametros del sistema").
 */
@Controller
@RequestMapping("/configuracion/ciclos-lectivos")
@PreAuthorize("hasRole('SUPERADMIN')")
@RequiredArgsConstructor
public class CicloLectivoController {

    private final CicloLectivoService cicloLectivoService;
    private final CierreCuatrimestreService cierreCuatrimestreService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("ciclos", cicloLectivoService.listar());
        return "configuracion/ciclos-lectivos";
    }

    @GetMapping("/nuevo")
    public String nuevoForm(Model model) {
        model.addAttribute("cicloLectivoForm", new CicloLectivoForm());
        return "configuracion/ciclo-lectivo-form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute CicloLectivoForm form, BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "configuracion/ciclo-lectivo-form";
        }
        cicloLectivoService.crear(form);
        redirectAttributes.addFlashAttribute("mensajeExito", "Ciclo lectivo creado.");
        return "redirect:/configuracion/ciclos-lectivos";
    }

    @PostMapping("/{id}/activar")
    public String activar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        cicloLectivoService.activar(id);
        // Seccion 5.14.2: despues de activar, definir en que curso queda cada alumno del año anterior.
        redirectAttributes.addFlashAttribute("mensajeExito",
                "Ciclo lectivo activado. Ahora indica en que curso queda cada alumno del año anterior.");
        return "redirect:/configuracion/promocion";
    }

    @PostMapping("/{id}/cuatrimestre/{numero}")
    public String cambiarCuatrimestre(@PathVariable Long id, @PathVariable int numero,
                                       RedirectAttributes redirectAttributes) {
        cicloLectivoService.cambiarCuatrimestre(id, numero);
        redirectAttributes.addFlashAttribute("mensajeExito", "Cuatrimestre activo actualizado.");
        return "redirect:/configuracion/ciclos-lectivos";
    }

    /** Seccion 6.4 paso 3: pantalla de verificacion previa antes de confirmar el cierre. */
    @GetMapping("/cerrar-cuatrimestre/{numero}")
    public String verificarCierre(@PathVariable int numero, Model model) {
        model.addAttribute("numero", numero);
        model.addAttribute("verificacion", cierreCuatrimestreService.verificar(numero));
        return "configuracion/cerrar-cuatrimestre";
    }

    @PostMapping("/cerrar-cuatrimestre/{numero}")
    public String confirmarCierre(@PathVariable int numero, Model model) {
        var paraRecuperacion = cierreCuatrimestreService.cerrar(numero);
        model.addAttribute("numero", numero);
        model.addAttribute("paraRecuperacion", paraRecuperacion);
        return "configuracion/cerrar-cuatrimestre-resultado";
    }
}
