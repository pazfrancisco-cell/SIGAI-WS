package ar.edu.wellspring.sigaiws.modules.disciplina.web;

import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoRepository;
import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.disciplina.dto.ActaForm;
import ar.edu.wellspring.sigaiws.modules.disciplina.dto.ObservacionBatchForm;
import ar.edu.wellspring.sigaiws.modules.disciplina.dto.ObservacionForm;
import ar.edu.wellspring.sigaiws.modules.disciplina.service.DisciplinaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Seccion 5.4.4 (Registro) y 5.4.6 (Gestion de Actas). */
@Controller
@RequestMapping("/disciplina")
@RequiredArgsConstructor
public class DisciplinaController {

    private final DisciplinaService disciplinaService;
    private final AlumnoRepository alumnoRepository;
    private final CursoDivisionRepository cursoDivisionRepository;

    @GetMapping("/nueva")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','PRECEPTOR','DOCENTE')")
    public String nuevaForm(Model model) {
        ObservacionBatchForm batchForm = new ObservacionBatchForm();
        batchForm.getObservaciones().add(new ObservacionForm());
        model.addAttribute("observacionBatchForm", batchForm);
        model.addAttribute("alumnos", alumnoRepository.findAll());
        return "disciplina/nueva";
    }

    @PostMapping("/nueva")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','PRECEPTOR','DOCENTE')")
    public String registrar(@Valid @ModelAttribute ObservacionBatchForm batchForm, BindingResult bindingResult,
                             @AuthenticationPrincipal SigaiUserPrincipal principal, Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("alumnos", alumnoRepository.findAll());
            return "disciplina/nueva";
        }
        int totalRegistradas = 0;
        for (ObservacionForm form : batchForm.getObservaciones()) {
            for (int i = 0; i < form.getCantidad(); i++) {
                disciplinaService.registrarObservacion(form, principal.getUsuario());
                totalRegistradas++;
            }
        }
        redirectAttributes.addFlashAttribute("mensajeExito",
                totalRegistradas == 1
                        ? "Observacion registrada correctamente."
                        : totalRegistradas + " observaciones registradas correctamente.");
        return "redirect:/disciplina/nueva";
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','PRECEPTOR')")
    public String resumenPorCurso(@RequestParam(required = false) Long cursoDivisionId, Model model) {
        var divisiones = cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc();
        Long division = cursoDivisionId != null ? cursoDivisionId : (divisiones.isEmpty() ? null : divisiones.get(0).getId());

        model.addAttribute("cursosDivisiones", divisiones);
        model.addAttribute("divisionSeleccionada", division);
        if (division != null) {
            model.addAttribute("trayectorias", disciplinaService.resumenPorDivision(division));
        }
        return "disciplina/resumen";
    }

    /** Seccion 5.4.3: "Solo el Administrativo puede eliminar o modificar una observacion ya registrada." */
    @GetMapping("/observaciones/{id}/editar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String editarObservacionForm(@org.springframework.web.bind.annotation.PathVariable Long id, Model model) {
        var observacion = disciplinaService.obtenerObservacion(id);
        ObservacionForm form = new ObservacionForm();
        form.setAlumnoId(observacion.getAlumno().getId());
        form.setFecha(observacion.getFecha());
        form.setCategoria(observacion.getCategoria());
        form.setDescripcion(observacion.getDescripcion());
        form.setFirmantesAdicionales(observacion.getFirmantesAdicionales());
        model.addAttribute("observacionForm", form);
        model.addAttribute("observacionId", id);
        model.addAttribute("alumno", observacion.getAlumno());
        return "disciplina/editar";
    }

    @PostMapping("/observaciones/{id}/editar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String editarObservacion(@org.springframework.web.bind.annotation.PathVariable Long id,
                                     @Valid @ModelAttribute ObservacionForm form, BindingResult bindingResult,
                                     Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("observacionId", id);
            model.addAttribute("alumno", disciplinaService.obtenerObservacion(id).getAlumno());
            return "disciplina/editar";
        }
        var observacion = disciplinaService.editarObservacion(id, form);
        redirectAttributes.addFlashAttribute("mensajeExito", "Observacion actualizada correctamente.");
        return "redirect:/alumnos/" + observacion.getAlumno().getId() + "#disciplina";
    }

    @PostMapping("/observaciones/{id}/eliminar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String eliminarObservacion(@org.springframework.web.bind.annotation.PathVariable Long id,
                                       RedirectAttributes redirectAttributes) {
        var alumnoId = disciplinaService.obtenerObservacion(id).getAlumno().getId();
        disciplinaService.eliminarObservacion(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Observacion eliminada correctamente.");
        return "redirect:/alumnos/" + alumnoId + "#disciplina";
    }

    @GetMapping("/actas")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String gestionActas(Model model) {
        model.addAttribute("alumnosConActaPendiente", disciplinaService.alumnosConActaPendiente());
        model.addAttribute("actaForm", new ActaForm());
        return "disciplina/actas";
    }

    @PostMapping("/actas")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String generarActa(@Valid @ModelAttribute ActaForm form, BindingResult bindingResult,
                               @AuthenticationPrincipal SigaiUserPrincipal principal, Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("alumnosConActaPendiente", disciplinaService.alumnosConActaPendiente());
            return "disciplina/actas";
        }
        disciplinaService.generarActa(form, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Acta generada y notificada a los tutores.");
        return "redirect:/disciplina/actas";
    }
}
