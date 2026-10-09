package ar.edu.wellspring.sigaiws.modules.calificacion.web;

import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.modules.alumno.service.AlumnoService;
import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.calificacion.domain.Evaluacion;
import ar.edu.wellspring.sigaiws.modules.calificacion.dto.CargaNotasForm;
import ar.edu.wellspring.sigaiws.modules.calificacion.dto.EvaluacionForm;
import ar.edu.wellspring.sigaiws.modules.calificacion.dto.SolicitudCorreccionForm;
import ar.edu.wellspring.sigaiws.modules.calificacion.repository.NotaRepository;
import ar.edu.wellspring.sigaiws.modules.calificacion.service.CalificacionService;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.MateriaRepository;
import ar.edu.wellspring.sigaiws.modules.horario.repository.AsignacionDocenteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Modulo 5.3. Docente: solo sus materias/divisiones asignadas (seccion 5.3.4). */
@Controller
@RequiredArgsConstructor
public class CalificacionController {

    private final CalificacionService calificacionService;
    private final MateriaRepository materiaRepository;
    private final CursoDivisionRepository cursoDivisionRepository;
    private final AlumnoService alumnoService;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final NotaRepository notaRepository;
    private final CicloLectivoRepository cicloLectivoRepository;

    @GetMapping("/calificaciones/cargar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','DOCENTE')")
    public String cargarForm(@RequestParam(required = false) Long materiaId,
                              @RequestParam(required = false) Long cursoDivisionId,
                              @RequestParam(required = false) Integer cuatrimestre,
                              @AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        boolean esSoloDocente = principal.getUsuario().tieneRol(RolNombre.DOCENTE);
        var cicloActivo = cicloLectivoRepository.findByActivoTrue().orElse(null);
        int cuatrimestreEfectivo = cuatrimestre != null ? cuatrimestre
                : cicloActivo != null ? cicloActivo.getCuatrimestreEnCurso() : 1;

        if (esSoloDocente) {
            var asignaciones = cicloActivo != null
                    ? asignacionDocenteRepository.findAllByDocente_IdAndCicloLectivo_Id(principal.getId(), cicloActivo.getId())
                    : List.<ar.edu.wellspring.sigaiws.modules.horario.domain.AsignacionDocente>of();
            model.addAttribute("materias", asignaciones.stream().map(a -> a.getMateria()).distinct().toList());
            model.addAttribute("cursosDivisiones", asignaciones.stream().map(a -> a.getCursoDivision()).distinct().toList());
        } else {
            model.addAttribute("materias", materiaRepository.findAllByActivaTrueOrderByNombre());
            model.addAttribute("cursosDivisiones", cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc());
        }

        model.addAttribute("materiaSeleccionada", materiaId);
        model.addAttribute("divisionSeleccionada", cursoDivisionId);
        model.addAttribute("cuatrimestre", cuatrimestreEfectivo);
        model.addAttribute("evaluacionForm", new EvaluacionForm());

        if (materiaId != null && cursoDivisionId != null) {
            List<Evaluacion> evaluaciones = calificacionService.evaluacionesDe(materiaId, cursoDivisionId, cuatrimestreEfectivo);
            var alumnos = alumnoService.alumnosDeDivisionEnCicloActivo(cursoDivisionId);
            model.addAttribute("evaluaciones", evaluaciones);
            model.addAttribute("alumnos", alumnos);

            // notasPorAlumnoYEvaluacion[alumnoId][evaluacionId] = valor, para lookup O(1) en la vista.
            java.util.Map<Long, java.util.Map<Long, java.math.BigDecimal>> notasPorAlumno = new java.util.HashMap<>();
            java.util.Map<Long, java.math.BigDecimal> promedios = new java.util.HashMap<>();
            for (var alumno : alumnos) {
                java.util.Map<Long, java.math.BigDecimal> porEvaluacion = new java.util.HashMap<>();
                for (Evaluacion ev : evaluaciones) {
                    notaRepository.findByEvaluacion_IdAndAlumno_Id(ev.getId(), alumno.getId())
                            .ifPresent(n -> porEvaluacion.put(ev.getId(), n.getValor()));
                }
                notasPorAlumno.put(alumno.getId(), porEvaluacion);
                promedios.put(alumno.getId(), calificacionService.promedioCuatrimestral(alumno.getId(), materiaId, cuatrimestreEfectivo));
            }
            model.addAttribute("notasPorAlumno", notasPorAlumno);
            model.addAttribute("promedios", promedios);
        }
        return "calificaciones/cargar";
    }

    @PostMapping("/calificaciones/evaluacion")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','DOCENTE')")
    public String crearEvaluacion(@ModelAttribute EvaluacionForm form, @AuthenticationPrincipal SigaiUserPrincipal principal,
                                   RedirectAttributes redirectAttributes) {
        calificacionService.crearEvaluacion(form, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Evaluacion creada.");
        return "redirect:/calificaciones/cargar?materiaId=" + form.getMateriaId() + "&cursoDivisionId=" + form.getCursoDivisionId()
                + "&cuatrimestre=" + form.getCuatrimestre();
    }

    @PostMapping("/calificaciones/evaluacion/editar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','DOCENTE')")
    public String editarEvaluacion(@RequestParam Long evaluacionId, @ModelAttribute EvaluacionForm form,
                                    @AuthenticationPrincipal SigaiUserPrincipal principal,
                                    RedirectAttributes redirectAttributes) {
        calificacionService.editarEvaluacion(evaluacionId, form, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Evaluacion actualizada.");
        return "redirect:/calificaciones/cargar?materiaId=" + form.getMateriaId() + "&cursoDivisionId=" + form.getCursoDivisionId()
                + "&cuatrimestre=" + form.getCuatrimestre();
    }

    @PostMapping("/calificaciones/evaluacion/{id}/eliminar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','DOCENTE')")
    public String eliminarEvaluacion(@PathVariable Long id, @RequestParam Long materiaId, @RequestParam Long cursoDivisionId,
                                      @RequestParam int cuatrimestre, @AuthenticationPrincipal SigaiUserPrincipal principal,
                                      RedirectAttributes redirectAttributes) {
        calificacionService.eliminarEvaluacion(id, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Evaluacion eliminada.");
        return "redirect:/calificaciones/cargar?materiaId=" + materiaId + "&cursoDivisionId=" + cursoDivisionId
                + "&cuatrimestre=" + cuatrimestre;
    }

    @PostMapping("/calificaciones/cargar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','DOCENTE')")
    public String guardarNotas(@ModelAttribute CargaNotasForm form, @AuthenticationPrincipal SigaiUserPrincipal principal,
                                @RequestParam Long materiaId, @RequestParam Long cursoDivisionId,
                                @RequestParam int cuatrimestre, RedirectAttributes redirectAttributes) {
        calificacionService.cargarNotas(form, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Notas guardadas.");
        return "redirect:/calificaciones/cargar?materiaId=" + materiaId + "&cursoDivisionId=" + cursoDivisionId
                + "&cuatrimestre=" + cuatrimestre;
    }

    /** Seccion 5.3.4: "El sistema guarda automaticamente cada nota individual al cambiar el foco del input (autosave)". */
    @PostMapping("/calificaciones/nota")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','DOCENTE')")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.Map<String, Object> guardarNotaAjax(@RequestParam Long evaluacionId, @RequestParam Long alumnoId,
                                                           @RequestParam String valor,
                                                           @AuthenticationPrincipal SigaiUserPrincipal principal) {
        try {
            CargaNotasForm form = new CargaNotasForm();
            form.setEvaluacionId(evaluacionId);
            CargaNotasForm.Item item = new CargaNotasForm.Item();
            item.setAlumnoId(alumnoId);
            item.setValor(valor);
            form.setItems(List.of(item));
            calificacionService.cargarNotas(form, principal.getUsuario());
            return java.util.Map.of("ok", true);
        } catch (RuntimeException e) {
            return java.util.Map.of("ok", false, "mensaje", e.getMessage() != null ? e.getMessage() : "No se pudo guardar la nota.");
        }
    }

    @PostMapping("/calificaciones/solicitar-correccion")
    @PreAuthorize("hasRole('DOCENTE')")
    public String solicitarCorreccion(@ModelAttribute SolicitudCorreccionForm form,
                                       @AuthenticationPrincipal SigaiUserPrincipal principal,
                                       RedirectAttributes redirectAttributes) {
        calificacionService.solicitarCorreccion(form, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Solicitud de correccion enviada al Administrativo.");
        return "redirect:/calificaciones/cargar";
    }

    @GetMapping("/calificaciones/solicitudes")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String solicitudes(Model model) {
        model.addAttribute("solicitudes", calificacionService.solicitudesPendientes());
        return "calificaciones/solicitudes";
    }

    @PostMapping("/calificaciones/solicitudes/{id}/aprobar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String aprobar(@PathVariable Long id, @AuthenticationPrincipal SigaiUserPrincipal principal,
                           RedirectAttributes redirectAttributes) {
        calificacionService.resolverSolicitud(id, true, null, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Correccion aprobada.");
        return "redirect:/calificaciones/solicitudes";
    }

    @PostMapping("/calificaciones/solicitudes/{id}/rechazar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String rechazar(@PathVariable Long id, @RequestParam String motivo,
                            @AuthenticationPrincipal SigaiUserPrincipal principal, RedirectAttributes redirectAttributes) {
        calificacionService.resolverSolicitud(id, false, motivo, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Correccion rechazada.");
        return "redirect:/calificaciones/solicitudes";
    }
}
