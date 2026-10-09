package ar.edu.wellspring.sigaiws.modules.reporte.web;

import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.reporte.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Seccion 5.8: generacion de reportes institucionales. */
@Controller
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @PostMapping("/alumnos/{id}/reportes/boletin")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String generarBoletin(@PathVariable Long id, @RequestParam int cuatrimestre,
                                  @AuthenticationPrincipal SigaiUserPrincipal principal,
                                  RedirectAttributes redirectAttributes) {
        var reporte = reporteService.generarBoletin(id, cuatrimestre, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Boletín generado correctamente.");
        redirectAttributes.addFlashAttribute("ultimoReportePath", reporte.getPathArchivo());
        return "redirect:/alumnos/" + id + "#academico";
    }

    @PostMapping("/alumnos/{id}/reportes/fip")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String generarFip(@PathVariable Long id, @RequestParam(required = false) String observaciones,
                              @AuthenticationPrincipal SigaiUserPrincipal principal,
                              RedirectAttributes redirectAttributes) {
        reporteService.generarFip(id, observaciones, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "FIP generada correctamente.");
        return "redirect:/alumnos/" + id + "#academico";
    }

    @PostMapping("/alumnos/{id}/reportes/rite")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String generarRite(@PathVariable Long id, @AuthenticationPrincipal SigaiUserPrincipal principal,
                               RedirectAttributes redirectAttributes) {
        reporteService.generarRite(id, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "RITE generado correctamente.");
        return "redirect:/alumnos/" + id + "#academico";
    }
}
