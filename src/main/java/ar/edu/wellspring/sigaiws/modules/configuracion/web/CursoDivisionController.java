package ar.edu.wellspring.sigaiws.modules.configuracion.web;

import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.service.CursoDivisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Seccion 5.14: gestion de cursos/divisiones. Mismo acceso que Materias. */
@Controller
@RequestMapping("/configuracion/cursos")
@PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
@RequiredArgsConstructor
public class CursoDivisionController {

    private final CursoDivisionService cursoDivisionService;
    private final CursoDivisionRepository cursoDivisionRepository;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("cursos", cursoDivisionService.listar());
        model.addAttribute("cursosActivos", cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc());
        return "configuracion/cursos";
    }

    @PostMapping
    public String crear(@RequestParam(required = false) Integer anio, @RequestParam(required = false) String division,
                        @RequestParam(required = false) Long copiarMateriasDeId, RedirectAttributes redirectAttributes) {
        return ejecutar(redirectAttributes, () -> {
            var curso = cursoDivisionService.crear(anio, division, copiarMateriasDeId);
            return "Curso " + curso.getNombreCompleto() + " creado.";
        });
    }

    @PostMapping("/{id}/division")
    public String cambiarDivision(@PathVariable Long id, @RequestParam String division, RedirectAttributes redirectAttributes) {
        return ejecutar(redirectAttributes, () -> {
            cursoDivisionService.cambiarDivision(id, division);
            return "Curso actualizado.";
        });
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return ejecutar(redirectAttributes, () -> {
            cursoDivisionService.desactivar(id);
            return "Curso desactivado: ya no aparece para cargas nuevas.";
        });
    }

    @PostMapping("/{id}/activar")
    public String activar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return ejecutar(redirectAttributes, () -> {
            cursoDivisionService.activar(id);
            return "Curso reactivado.";
        });
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            cursoDivisionService.eliminar(id);
            redirectAttributes.addFlashAttribute("mensajeExito", "Curso eliminado.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("errorMensaje",
                    "No se puede eliminar: el curso tiene datos asociados (alumnos, horarios, notas, etc.). Podes desactivarlo.");
        }
        return "redirect:/configuracion/cursos";
    }

    private String ejecutar(RedirectAttributes redirectAttributes, java.util.function.Supplier<String> accion) {
        try {
            redirectAttributes.addFlashAttribute("mensajeExito", accion.get());
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMensaje", e.getMessage());
        }
        return "redirect:/configuracion/cursos";
    }
}
