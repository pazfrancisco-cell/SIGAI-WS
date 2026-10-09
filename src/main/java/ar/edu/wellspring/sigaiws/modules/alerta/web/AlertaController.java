package ar.edu.wellspring.sigaiws.modules.alerta.web;

import ar.edu.wellspring.sigaiws.modules.alerta.domain.Alerta;
import ar.edu.wellspring.sigaiws.modules.alerta.service.AlertaService;
import ar.edu.wellspring.sigaiws.modules.configuracion.service.AvisoCierreCuatrimestreService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Seccion 5.13.3 (Centro de Alertas). */
@Controller
@RequestMapping("/alertas")
@PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','PRECEPTOR')")
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaService alertaService;
    private final AvisoCierreCuatrimestreService avisoCierreCuatrimestreService;

    @GetMapping
    public String centroDeAlertas(@RequestParam(required = false) Alerta.Prioridad prioridad,
                                   @RequestParam(required = false) String tipo, Model model) {
        model.addAttribute("alertas", alertaService.activas(prioridad, tipo));
        model.addAttribute("tiposDisponibles", alertaService.tiposDeAlertasActivas());
        model.addAttribute("prioridadSeleccionada", prioridad);
        model.addAttribute("tipoSeleccionado", tipo);
        return "alertas/centro";
    }

    @GetMapping("/historial")
    public String historial(Model model) {
        model.addAttribute("alertas", alertaService.historial());
        return "alertas/historial";
    }

    /** Disparo manual del chequeo diario de cierre de cuatrimestre proximo (ademas del @Scheduled de las 7:05). */
    @PostMapping("/verificar-cierre")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String verificarCierre(RedirectAttributes redirectAttributes) {
        var r = avisoCierreCuatrimestreService.verificar();
        String cuando = r.diasRestantes() == 0 ? "cierra hoy"
                : r.diasRestantes() == 1 ? "cierra mañana" : "faltan " + r.diasRestantes() + " días";
        String mensaje = !r.enVentana()
                ? "No hay un cierre de cuatrimestre en los próximos 7 días."
                : "Cuatrimestre %d: %s. %s Docentes avisados ahora: %d.".formatted(r.cuatrimestre(), cuando,
                        r.alertaCreada() ? "Se generó la alerta." : "La alerta ya existía.", r.docentesAvisados());
        redirectAttributes.addFlashAttribute("mensajeExito", mensaje);
        return "redirect:/alertas";
    }

    @PostMapping("/{id}/seguimiento")
    public String marcarEnSeguimiento(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        alertaService.marcarEnSeguimiento(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Alerta marcada en seguimiento.");
        return "redirect:/alertas";
    }

    @PostMapping("/{id}/resolver")
    public String resolver(@PathVariable Long id, @RequestParam String notaResolucion,
                            RedirectAttributes redirectAttributes) {
        alertaService.resolver(id, notaResolucion);
        redirectAttributes.addFlashAttribute("mensajeExito", "Alerta resuelta.");
        return "redirect:/alertas";
    }
}
