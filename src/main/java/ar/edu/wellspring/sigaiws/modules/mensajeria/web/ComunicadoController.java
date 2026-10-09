package ar.edu.wellspring.sigaiws.modules.mensajeria.web;

import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoRepository;
import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.mensajeria.dto.ComunicadoForm;
import ar.edu.wellspring.sigaiws.modules.mensajeria.service.ComunicadoService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Seccion 5.10.4 (Nuevo Comunicado Institucional). Acceso: Administrativo, Directivo. */
@Controller
@RequestMapping("/mensajes/comunicado")
@PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
@RequiredArgsConstructor
public class ComunicadoController {

    private final ComunicadoService comunicadoService;
    private final CursoDivisionRepository cursoDivisionRepository;
    private final AlumnoRepository alumnoRepository;

    @GetMapping
    public String listar(Model model) {
        var comunicados = comunicadoService.listar();
        model.addAttribute("comunicados", comunicados);
        Map<Long, ComunicadoService.ResumenLectura> resumenes = new HashMap<>();
        for (var c : comunicados) {
            resumenes.put(c.getId(), comunicadoService.resumenLectura(c.getId()));
        }
        model.addAttribute("resumenes", resumenes);
        return "mensajes/comunicados";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        model.addAttribute("comunicado", comunicadoService.obtener(id));
        model.addAttribute("lecturas", comunicadoService.lecturasDe(id));
        model.addAttribute("pendientes", comunicadoService.pendientesDe(id));
        model.addAttribute("resumen", comunicadoService.resumenLectura(id));
        return "mensajes/comunicado-detalle";
    }

    @GetMapping("/nuevo")
    public String nuevoForm(Model model) {
        model.addAttribute("comunicadoForm", new ComunicadoForm());
        model.addAttribute("cursosDivisiones", cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc());
        model.addAttribute("alumnos", alumnoRepository.findAll());
        return "mensajes/comunicado-nuevo";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute ComunicadoForm form, BindingResult bindingResult,
                         @AuthenticationPrincipal SigaiUserPrincipal principal, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("cursosDivisiones", cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc());
            model.addAttribute("alumnos", alumnoRepository.findAll());
            return "mensajes/comunicado-nuevo";
        }
        comunicadoService.crear(form, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Comunicado enviado.");
        return "redirect:/mensajes/comunicado";
    }
}
