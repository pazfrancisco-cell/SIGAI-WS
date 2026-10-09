package ar.edu.wellspring.sigaiws.modules.calificacion.web;

import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.calificacion.domain.MateriaAdeudada;
import ar.edu.wellspring.sigaiws.modules.calificacion.domain.NotaMesa;
import ar.edu.wellspring.sigaiws.modules.calificacion.domain.TurnoMesa;
import ar.edu.wellspring.sigaiws.modules.calificacion.service.MesaExamenService;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.service.ParametroAcademicoService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

/** Mesas de examen (diciembre, febrero, julio): carga de notas de materias adeudadas por el docente de la materia o gestion. */
@Controller
@RequestMapping("/mesas")
@PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','DOCENTE')")
@RequiredArgsConstructor
public class MesaExamenController {

    private final MesaExamenService mesaExamenService;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final ParametroAcademicoService parametroAcademicoService;

    public record FilaMesa(MateriaAdeudada adeudada, NotaMesa notaCargada) {}

    @InitBinder
    void permitirListasLargas(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(5000);
    }

    @GetMapping
    public String pantalla(@RequestParam(required = false) TurnoMesa turno,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                           @RequestParam(required = false) Long materiaId,
                           @RequestParam(required = false) Integer anioCurso,
                           @AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        LocalDate fechaMesa = fecha == null || fecha.isAfter(LocalDate.now()) ? LocalDate.now() : fecha;
        TurnoMesa turnoMesa = turno == null ? TurnoMesa.segun(fechaMesa) : turno;
        var usuario = principal.getUsuario();

        List<FilaMesa> filas = mesaExamenService.paraMesa(turnoMesa, fechaMesa.getYear(), materiaId, anioCurso, usuario).stream()
                .map(a -> new FilaMesa(a, mesaExamenService.notaCargada(a.getId(), turnoMesa, fechaMesa.getYear()).orElse(null)))
                .toList();

        model.addAttribute("turnos", TurnoMesa.values());
        model.addAttribute("turno", turnoMesa);
        model.addAttribute("fecha", fechaMesa);
        model.addAttribute("hoy", LocalDate.now());
        model.addAttribute("materias", mesaExamenService.materiasDisponibles(usuario));
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("anioCurso", anioCurso);
        model.addAttribute("filas", filas);
        model.addAttribute("esGestion", MesaExamenService.esGestion(usuario));
        model.addAttribute("cicloActivo", cicloLectivoRepository.findByActivoTrue().orElse(null));
        model.addAttribute("notaMinima", parametroAcademicoService.obtener().getNotaMinimaAprobacion());
        return "calificaciones/mesas";
    }

    @PostMapping
    public String cargar(@ModelAttribute MesaExamenService.CargaForm form,
                         @RequestParam(required = false) Long materiaId,
                         @RequestParam(required = false) Integer anioCurso,
                         @AuthenticationPrincipal SigaiUserPrincipal principal, RedirectAttributes redirectAttributes) {
        try {
            var r = mesaExamenService.cargar(form, principal.getUsuario());
            String mensaje = "Mesa guardada: " + r.cargadas() + " notas cargadas"
                    + (r.aprobadas() > 0 ? ", " + r.aprobadas() + " materias aprobadas" : "")
                    + (r.borradas() > 0 ? ", " + r.borradas() + " notas borradas" : "") + ".";
            redirectAttributes.addFlashAttribute("mensajeExito", mensaje);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMensaje", e.getMessage());
        }
        return "redirect:" + UriComponentsBuilder.fromPath("/mesas")
                .queryParamIfPresent("turno", java.util.Optional.ofNullable(form.getTurno()))
                .queryParamIfPresent("fecha", java.util.Optional.ofNullable(form.getFecha()))
                .queryParamIfPresent("materiaId", java.util.Optional.ofNullable(materiaId))
                .queryParamIfPresent("anioCurso", java.util.Optional.ofNullable(anioCurso))
                .build().toUriString();
    }

    /** Vuelve a calcular las materias adeudadas del ciclo activo con las notas actuales (por si se corrigieron notas despues del cierre). */
    @PostMapping("/sincronizar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String sincronizar(RedirectAttributes redirectAttributes) {
        cicloLectivoRepository.findByActivoTrue().ifPresentOrElse(ciclo -> {
            var r = mesaExamenService.sincronizarCiclo(ciclo);
            redirectAttributes.addFlashAttribute("mensajeExito", "Materias adeudadas de %d actualizadas: %d nuevas, %d con promedio corregido, %d quitadas."
                    .formatted(ciclo.getAnio(), r.nuevas(), r.actualizadas(), r.quitadas()));
        }, () -> redirectAttributes.addFlashAttribute("errorMensaje", "No hay un ciclo lectivo activo."));
        return "redirect:/mesas";
    }
}
