package ar.edu.wellspring.sigaiws.modules.horario.web;

import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.MateriaRepository;
import ar.edu.wellspring.sigaiws.modules.horario.dto.AsignacionDocenteForm;
import ar.edu.wellspring.sigaiws.modules.horario.service.AsignacionDocenteService;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

/** Seccion 5.5.5: /horarios/asignaciones, Acceso Administrativo/Directivo/Superadmin. */
@Controller
@RequestMapping("/horarios/asignaciones")
@PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
@RequiredArgsConstructor
public class AsignacionDocenteController {

    private final AsignacionDocenteService asignacionDocenteService;
    private final UsuarioRepository usuarioRepository;
    private final MateriaRepository materiaRepository;
    private final CursoDivisionRepository cursoDivisionRepository;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("asignaciones", asignacionDocenteService.listarDelCicloActivo());
        model.addAttribute("asignacionDocenteForm", new AsignacionDocenteForm());
        cargarDatos(model);
        return "horarios/asignaciones";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute AsignacionDocenteForm form, BindingResult bindingResult,
                         Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("asignaciones", asignacionDocenteService.listarDelCicloActivo());
            cargarDatos(model);
            return "horarios/asignaciones";
        }
        asignacionDocenteService.crear(form);
        redirectAttributes.addFlashAttribute("mensajeExito", "Asignacion creada.");
        return "redirect:/horarios/asignaciones";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        asignacionDocenteService.eliminar(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Asignacion eliminada.");
        return "redirect:/horarios/asignaciones";
    }

    private void cargarDatos(Model model) {
        model.addAttribute("docentes", usuarioRepository.buscar(null, true, null, PageRequest.of(0, 200)).getContent()
                .stream().filter(u -> u.tieneRol(RolNombre.DOCENTE)).toList());
        model.addAttribute("materias", materiaRepository.findAllByActivaTrueOrderByNombre());
        model.addAttribute("cursosDivisiones", cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc());
    }
}
