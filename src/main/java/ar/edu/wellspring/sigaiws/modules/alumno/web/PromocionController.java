package ar.edu.wellspring.sigaiws.modules.alumno.web;

import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.modules.alumno.service.PromocionService;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Seccion 5.14.2: asistente de promocion de alumnos al empezar un ciclo lectivo (decide quien repite: Directivo). */
@Controller
@RequestMapping("/configuracion/promocion")
@PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO')")
@RequiredArgsConstructor
public class PromocionController {

    private final PromocionService promocionService;
    private final CursoDivisionRepository cursoDivisionRepository;

    /** Por defecto Spring corta las listas del formulario en 256 elementos; un colegio puede tener mas alumnos. */
    @org.springframework.web.bind.annotation.InitBinder
    void permitirListasLargas(org.springframework.web.bind.WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(5000);
    }

    @GetMapping
    public String asistente(Model model) {
        model.addAttribute("propuesta", promocionService.propuesta());
        model.addAttribute("cursosActivos", cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc());
        return "configuracion/promocion";
    }

    @PostMapping
    public String confirmar(@ModelAttribute PromocionService.PromocionForm form, RedirectAttributes redirectAttributes) {
        try {
            var r = promocionService.confirmar(form.getDecisiones());
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Promocion aplicada: %d pasan de año, %d repiten, %d egresan.".formatted(r.pasaron(), r.repiten(), r.egresaron())
                            + (r.yaInscriptos() > 0 ? " " + r.yaInscriptos() + " ya tenian curso y no se modificaron." : ""));
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMensaje", e.getMessage());
        }
        return "redirect:/configuracion/promocion";
    }
}
