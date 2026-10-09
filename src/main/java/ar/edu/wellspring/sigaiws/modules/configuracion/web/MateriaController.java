package ar.edu.wellspring.sigaiws.modules.configuracion.web;

import ar.edu.wellspring.sigaiws.modules.configuracion.dto.MateriaForm;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.service.MateriaService;
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
 * Acceso inferido (docs/DECISIONS.md ADR-007): la estructura academica base
 * (materias, aulas, workshops) se administra desde Superadmin/Directivo/
 * Administrativo, coherente con quienes gestionan horarios y asignaciones (5.12.2).
 */
@Controller
@RequestMapping("/configuracion/materias")
@PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
@RequiredArgsConstructor
public class MateriaController {

    private final MateriaService materiaService;
    private final CursoDivisionRepository cursoDivisionRepository;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("materias", materiaService.listar());
        return "configuracion/materias";
    }

    @GetMapping("/nuevo")
    public String nuevoForm(Model model) {
        model.addAttribute("materiaForm", new MateriaForm());
        model.addAttribute("cursosDivisiones", cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc());
        return "configuracion/materia-form";
    }

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable Long id, Model model) {
        var materia = materiaService.obtener(id);
        MateriaForm form = new MateriaForm();
        form.setId(materia.getId());
        form.setNombre(materia.getNombre());
        form.setActiva(materia.isActiva());
        form.setCursoDivisionIds(materia.getCursosDivisiones().stream()
                .map(cd -> cd.getId()).collect(java.util.stream.Collectors.toSet()));
        model.addAttribute("materiaForm", form);
        model.addAttribute("cursosDivisiones", cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc());
        return "configuracion/materia-form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute MateriaForm form, BindingResult bindingResult,
                           Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("cursosDivisiones", cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc());
            return "configuracion/materia-form";
        }
        materiaService.guardar(form);
        redirectAttributes.addFlashAttribute("mensajeExito", "Materia guardada correctamente.");
        return "redirect:/configuracion/materias";
    }
}
