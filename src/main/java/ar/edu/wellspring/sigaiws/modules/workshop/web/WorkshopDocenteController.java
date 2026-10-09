package ar.edu.wellspring.sigaiws.modules.workshop.web;

import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.WorkshopRepository;
import ar.edu.wellspring.sigaiws.modules.workshop.domain.WorkshopEvaluacion;
import ar.edu.wellspring.sigaiws.modules.workshop.dto.WorkshopAsistenciaBatchForm;
import ar.edu.wellspring.sigaiws.modules.workshop.dto.WorkshopCargaNotasForm;
import ar.edu.wellspring.sigaiws.modules.workshop.dto.WorkshopEvaluacionForm;
import ar.edu.wellspring.sigaiws.modules.workshop.repository.WorkshopDocenteRepository;
import ar.edu.wellspring.sigaiws.modules.workshop.repository.WorkshopNotaRepository;
import ar.edu.wellspring.sigaiws.modules.workshop.service.WorkshopAsistenciaService;
import ar.edu.wellspring.sigaiws.modules.workshop.service.WorkshopCalificacionService;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Fase 4 (seccion 5.6): calificaciones y asistencia de workshop. Docente: solo
 * su propio workshop asignado (equivalente al "esSoloDocente" de Calificaciones/Asistencia).
 */
@Controller
@RequiredArgsConstructor
public class WorkshopDocenteController {

    private final WorkshopCalificacionService workshopCalificacionService;
    private final WorkshopAsistenciaService workshopAsistenciaService;
    private final WorkshopRepository workshopRepository;
    private final WorkshopDocenteRepository workshopDocenteRepository;
    private final WorkshopNotaRepository workshopNotaRepository;
    private final CicloLectivoRepository cicloLectivoRepository;

    @GetMapping("/workshops/calificaciones")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO') or (hasRole('DOCENTE') and principal.usuario.esDocenteWorkshop)")
    public String calificacionesForm(@RequestParam(required = false) Long workshopId,
                                      @RequestParam(required = false) Integer cuatrimestre,
                                      @RequestParam(required = false) Long evaluacionId,
                                      @AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        boolean esSoloDocente = principal.getUsuario().tieneRol(RolNombre.DOCENTE);
        CicloLectivo cicloActivo = cicloLectivoRepository.findByActivoTrue().orElse(null);
        int cuatrimestreEfectivo = cuatrimestre != null ? cuatrimestre
                : cicloActivo != null ? cicloActivo.getCuatrimestreEnCurso() : 1;

        if (esSoloDocente) {
            var propio = workshopDocenteRepository.findByDocente_Id(principal.getId());
            model.addAttribute("workshops", propio.map(wd -> List.of(wd.getWorkshop())).orElse(List.of()));
            // Defensa en profundidad: un docente solo puede ver SU workshop, sin importar que workshopId le pasen por query string.
            workshopId = propio.map(wd -> wd.getWorkshop().getId()).orElse(null);
        } else {
            model.addAttribute("workshops", workshopRepository.findAllByActivoTrueOrderByNombre());
        }

        model.addAttribute("workshopSeleccionado", workshopId);
        model.addAttribute("cuatrimestre", cuatrimestreEfectivo);
        model.addAttribute("evaluacionForm", new WorkshopEvaluacionForm());

        if (workshopId != null) {
            List<WorkshopEvaluacion> evaluaciones = workshopCalificacionService.evaluacionesDe(workshopId, cuatrimestreEfectivo);
            var alumnos = workshopAsistenciaService.alumnosDelWorkshop(workshopId, cuatrimestreEfectivo);
            model.addAttribute("evaluaciones", evaluaciones);
            model.addAttribute("alumnos", alumnos);

            Long evaluacionSeleccionada = evaluacionId != null ? evaluacionId
                    : evaluaciones.isEmpty() ? null : evaluaciones.get(0).getId();
            model.addAttribute("evaluacionSeleccionada", evaluacionSeleccionada);

            Map<Long, String> notaPorAlumno = new HashMap<>();
            if (evaluacionSeleccionada != null) {
                for (var alumno : alumnos) {
                    workshopNotaRepository.findByEvaluacion_IdAndAlumno_Id(evaluacionSeleccionada, alumno.getId())
                            .ifPresent(n -> notaPorAlumno.put(alumno.getId(), n.getTexto()));
                }
            }
            model.addAttribute("notaPorAlumno", notaPorAlumno);
        }
        return "workshops/calificaciones";
    }

    @PostMapping("/workshops/evaluacion")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO') or (hasRole('DOCENTE') and principal.usuario.esDocenteWorkshop)")
    public String crearEvaluacion(@ModelAttribute WorkshopEvaluacionForm form, @AuthenticationPrincipal SigaiUserPrincipal principal,
                                   RedirectAttributes redirectAttributes) {
        workshopCalificacionService.crearEvaluacion(form, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Evaluacion creada.");
        return "redirect:/workshops/calificaciones?workshopId=" + form.getWorkshopId() + "&cuatrimestre=" + form.getCuatrimestre();
    }

    @PostMapping("/workshops/calificaciones")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO') or (hasRole('DOCENTE') and principal.usuario.esDocenteWorkshop)")
    public String guardarNotas(@ModelAttribute WorkshopCargaNotasForm form, @AuthenticationPrincipal SigaiUserPrincipal principal,
                                @RequestParam Long workshopId, @RequestParam int cuatrimestre,
                                RedirectAttributes redirectAttributes) {
        workshopCalificacionService.cargarNotas(form, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Notas guardadas.");
        return "redirect:/workshops/calificaciones?workshopId=" + workshopId + "&cuatrimestre=" + cuatrimestre;
    }

    @GetMapping("/workshops/asistencia")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO') or (hasRole('DOCENTE') and principal.usuario.esDocenteWorkshop)")
    public String asistenciaForm(@RequestParam(required = false) Long workshopId,
                                  @RequestParam(required = false) Integer cuatrimestre,
                                  @AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        boolean esSoloDocente = principal.getUsuario().tieneRol(RolNombre.DOCENTE);
        CicloLectivo cicloActivo = cicloLectivoRepository.findByActivoTrue().orElse(null);
        int cuatrimestreEfectivo = cuatrimestre != null ? cuatrimestre
                : cicloActivo != null ? cicloActivo.getCuatrimestreEnCurso() : 1;

        if (esSoloDocente) {
            var propio = workshopDocenteRepository.findByDocente_Id(principal.getId());
            model.addAttribute("workshops", propio.map(wd -> List.of(wd.getWorkshop())).orElse(List.of()));
            // Defensa en profundidad: un docente solo puede ver SU workshop, sin importar que workshopId le pasen por query string.
            workshopId = propio.map(wd -> wd.getWorkshop().getId()).orElse(null);
        } else {
            model.addAttribute("workshops", workshopRepository.findAllByActivoTrueOrderByNombre());
        }

        model.addAttribute("workshopSeleccionado", workshopId);
        model.addAttribute("cuatrimestre", cuatrimestreEfectivo);
        model.addAttribute("fecha", LocalDate.now());
        if (workshopId != null) {
            model.addAttribute("alumnos", workshopAsistenciaService.alumnosDelWorkshop(workshopId, cuatrimestreEfectivo));
        }
        return "workshops/asistencia";
    }

    @PostMapping("/workshops/asistencia")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO') or (hasRole('DOCENTE') and principal.usuario.esDocenteWorkshop)")
    public String guardarAsistencia(@ModelAttribute WorkshopAsistenciaBatchForm form, @AuthenticationPrincipal SigaiUserPrincipal principal,
                                     RedirectAttributes redirectAttributes) {
        workshopAsistenciaService.registrarClase(form, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Asistencia del workshop guardada.");
        return "redirect:/workshops/asistencia?workshopId=" + form.getWorkshopId() + "&cuatrimestre=" + form.getCuatrimestre();
    }
}
