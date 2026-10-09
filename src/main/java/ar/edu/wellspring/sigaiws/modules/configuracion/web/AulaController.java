package ar.edu.wellspring.sigaiws.modules.configuracion.web;

import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.PeriodoHorario;
import ar.edu.wellspring.sigaiws.modules.configuracion.dto.AulaForm;
import ar.edu.wellspring.sigaiws.modules.configuracion.dto.ReservaAulaForm;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.PeriodoHorarioRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.service.AulaService;
import ar.edu.wellspring.sigaiws.modules.configuracion.service.ReservaAulaService;
import jakarta.validation.Valid;
import java.time.LocalDate;
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

/** Seccion 5.5.1: administracion de aulas (Superadmin/Directivo/Administrativo) y reserva puntual (+ Docente). */
@Controller
@RequestMapping("/configuracion/aulas")
@PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','DOCENTE')")
@RequiredArgsConstructor
public class AulaController {

    private final AulaService aulaService;
    private final ReservaAulaService reservaAulaService;
    private final CursoDivisionRepository cursoDivisionRepository;
    private final PeriodoHorarioRepository periodoHorarioRepository;

    @GetMapping
    public String listar(@AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        model.addAttribute("aulas", aulaService.listar());
        model.addAttribute("cursosDivisiones", cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc());
        var periodos = periodoHorarioRepository.findAllByTipoOrderByOrden(PeriodoHorario.Tipo.CLASE);
        model.addAttribute("periodos", periodos);
        Map<Integer, PeriodoHorario> periodoPorOrden = new HashMap<>();
        for (var p : periodos) {
            periodoPorOrden.put(p.getOrden(), p);
        }
        model.addAttribute("periodoPorOrden", periodoPorOrden);
        model.addAttribute("hoy", LocalDate.now());
        model.addAttribute("reservaForm", new ReservaAulaForm());
        model.addAttribute("misReservas", reservaAulaService.misReservas(principal.getId()));
        return "configuracion/aulas";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String nuevoForm(Model model) {
        model.addAttribute("aulaForm", new AulaForm());
        return "configuracion/aula-form";
    }

    @GetMapping("/{id}/editar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String editarForm(@PathVariable Long id, Model model) {
        var aula = aulaService.obtener(id);
        AulaForm form = new AulaForm();
        form.setId(aula.getId());
        form.setNombre(aula.getNombre());
        form.setCapacidad(aula.getCapacidad());
        form.setActiva(aula.isActiva());
        model.addAttribute("aulaForm", form);
        return "configuracion/aula-form";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String guardar(@Valid @ModelAttribute AulaForm form, BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "configuracion/aula-form";
        }
        aulaService.guardar(form);
        redirectAttributes.addFlashAttribute("mensajeExito", "Aula guardada correctamente.");
        return "redirect:/configuracion/aulas";
    }

    @PostMapping("/{id}/eliminar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        aulaService.eliminar(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Aula eliminada.");
        return "redirect:/configuracion/aulas";
    }

    @PostMapping("/reservar")
    public String reservar(@ModelAttribute ReservaAulaForm form, @AuthenticationPrincipal SigaiUserPrincipal principal,
                            RedirectAttributes redirectAttributes) {
        reservaAulaService.reservar(form, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Aula reservada correctamente.");
        return "redirect:/configuracion/aulas";
    }

    @PostMapping("/reservas/{id}/cancelar")
    public String cancelarReserva(@PathVariable Long id, @AuthenticationPrincipal SigaiUserPrincipal principal,
                                   RedirectAttributes redirectAttributes) {
        reservaAulaService.cancelar(id, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Reserva cancelada.");
        return "redirect:/configuracion/aulas";
    }
}
