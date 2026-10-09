package ar.edu.wellspring.sigaiws.modules.workshop.web;

import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.PeriodoHorario;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.AulaRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.PeriodoHorarioRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.WorkshopRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import ar.edu.wellspring.sigaiws.modules.workshop.dto.WorkshopSlotForm;
import ar.edu.wellspring.sigaiws.modules.workshop.repository.WorkshopDocenteRepository;
import ar.edu.wellspring.sigaiws.modules.workshop.service.WorkshopAsignacionService;
import ar.edu.wellspring.sigaiws.modules.workshop.service.WorkshopHorarioService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Seccion 5.6.3/5.6.4. */
@Controller
@RequestMapping("/workshops")
@PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
@RequiredArgsConstructor
public class WorkshopAsignacionController {

    private final WorkshopAsignacionService workshopAsignacionService;
    private final WorkshopHorarioService workshopHorarioService;
    private final WorkshopRepository workshopRepository;
    private final WorkshopDocenteRepository workshopDocenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CursoDivisionRepository cursoDivisionRepository;
    private final AulaRepository aulaRepository;
    private final PeriodoHorarioRepository periodoHorarioRepository;

    @GetMapping
    public String listar(Model model) {
        var workshops = workshopRepository.findAllByActivoTrueOrderByNombre();
        model.addAttribute("workshops", workshops);

        Map<Long, String> docentePorWorkshop = new HashMap<>();
        for (var wd : workshopDocenteRepository.findAll()) {
            docentePorWorkshop.put(wd.getWorkshop().getId(), wd.getDocente().getNombreCompleto());
        }
        model.addAttribute("docentePorWorkshop", docentePorWorkshop);

        /** Seccion 5.6.3: "cantidad de alumnos en Cuatrimestre 1, cantidad de alumnos en Cuatrimestre 2". */
        Map<Long, Integer> cantidadC1 = new HashMap<>();
        Map<Long, Integer> cantidadC2 = new HashMap<>();
        for (var w : workshops) {
            cantidadC1.put(w.getId(), workshopAsignacionService.alumnosDelWorkshop(w.getId(), 1).size());
            cantidadC2.put(w.getId(), workshopAsignacionService.alumnosDelWorkshop(w.getId(), 2).size());
        }
        model.addAttribute("cantidadC1", cantidadC1);
        model.addAttribute("cantidadC2", cantidadC2);

        model.addAttribute("docentesDisponibles", usuarioRepository.buscar(null, true, null, PageRequest.of(0, 200)).getContent()
                .stream().filter(u -> u.tieneRol(RolNombre.DOCENTE)).filter(u -> !workshopDocenteRepository.existsByDocente_Id(u.getId())).toList());
        return "workshops/listado";
    }

    /** Seccion 5.6.3: "Card de cada workshop expandible para ver lista de alumnos inscritos". */
    @GetMapping("/{id}/alumnos")
    public String alumnos(@PathVariable Long id, Model model) {
        var workshop = workshopRepository.findById(id).orElseThrow(() -> NotFoundException.of("Workshop", id));
        model.addAttribute("workshop", workshop);
        model.addAttribute("alumnosC1", workshopAsignacionService.alumnosDelWorkshop(id, 1));
        model.addAttribute("alumnosC2", workshopAsignacionService.alumnosDelWorkshop(id, 2));
        return "workshops/alumnos";
    }

    @PostMapping("/{id}/docente")
    public String asignarDocente(@org.springframework.web.bind.annotation.PathVariable Long id,
                                  @RequestParam Long docenteId, RedirectAttributes redirectAttributes) {
        workshopAsignacionService.asignarDocente(id, docenteId);
        redirectAttributes.addFlashAttribute("mensajeExito", "Docente asignado al workshop.");
        return "redirect:/workshops";
    }

    @GetMapping("/asignacion-masiva")
    public String asignacionMasiva(@RequestParam(required = false) Long cursoDivisionId, Model model) {
        model.addAttribute("alumnosSinC2", workshopAsignacionService.alumnosSinWorkshopC2(cursoDivisionId));
        model.addAttribute("workshops", workshopRepository.findAllByActivoTrueOrderByNombre());
        model.addAttribute("cursosDivisiones", cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc());
        model.addAttribute("cursoDivisionSeleccionado", cursoDivisionId);
        return "workshops/asignacion-masiva";
    }

    @PostMapping("/asignacion-masiva")
    public String guardarAsignacionMasiva(@RequestParam("alumnoCicloLectivoId") java.util.List<Long> ids,
                                           @RequestParam("workshopId") java.util.List<String> workshopIds,
                                           @RequestParam(required = false) Long cursoDivisionId,
                                           RedirectAttributes redirectAttributes) {
        for (int i = 0; i < ids.size(); i++) {
            String valor = workshopIds.get(i);
            if (valor != null && !valor.isBlank()) {
                workshopAsignacionService.asignarAlumnoC2(ids.get(i), Long.valueOf(valor));
            }
        }
        redirectAttributes.addFlashAttribute("mensajeExito", "Workshops del Segundo Cuatrimestre asignados.");
        return "redirect:/workshops/asignacion-masiva" + (cursoDivisionId != null ? "?cursoDivisionId=" + cursoDivisionId : "");
    }

    /** Fase 4: asignacion inicial del workshop de Primer Cuatrimestre (antes no existia ninguna pantalla para esto). */
    @GetMapping("/asignacion-masiva-c1")
    public String asignacionMasivaC1(@RequestParam(required = false) Long cursoDivisionId, Model model) {
        model.addAttribute("alumnosSinC1", workshopAsignacionService.alumnosSinWorkshopC1(cursoDivisionId));
        model.addAttribute("workshops", workshopRepository.findAllByActivoTrueOrderByNombre());
        model.addAttribute("cursosDivisiones", cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc());
        model.addAttribute("cursoDivisionSeleccionado", cursoDivisionId);
        return "workshops/asignacion-masiva-c1";
    }

    @PostMapping("/asignacion-masiva-c1")
    public String guardarAsignacionMasivaC1(@RequestParam("alumnoCicloLectivoId") java.util.List<Long> ids,
                                             @RequestParam("workshopId") java.util.List<String> workshopIds,
                                             @RequestParam(required = false) Long cursoDivisionId,
                                             RedirectAttributes redirectAttributes) {
        for (int i = 0; i < ids.size(); i++) {
            String valor = workshopIds.get(i);
            if (valor != null && !valor.isBlank()) {
                workshopAsignacionService.asignarAlumnoC1(ids.get(i), Long.valueOf(valor));
            }
        }
        redirectAttributes.addFlashAttribute("mensajeExito", "Workshops del Primer Cuatrimestre asignados.");
        return "redirect:/workshops/asignacion-masiva-c1" + (cursoDivisionId != null ? "?cursoDivisionId=" + cursoDivisionId : "");
    }

    /** Fase 4: horario fijo semanal del workshop (seccion 5.6, reutiliza el chequeo de conflictos de 5.5.3). */
    @GetMapping("/{id}/horario")
    public String horario(@PathVariable Long id, Model model) {
        var workshop = workshopRepository.findById(id).orElseThrow(() -> NotFoundException.of("Workshop", id));
        model.addAttribute("workshop", workshop);
        model.addAttribute("slots", workshopHorarioService.gridDelWorkshop(id));
        model.addAttribute("periodos", periodoHorarioRepository.findAllByTipoOrderByOrden(PeriodoHorario.Tipo.CLASE));
        model.addAttribute("aulas", aulaRepository.findAllByActivaTrueOrderByNombre());
        return "workshops/horario";
    }

    @PostMapping("/{id}/horario")
    public String guardarHorario(@PathVariable Long id, @ModelAttribute WorkshopSlotForm form,
                                  RedirectAttributes redirectAttributes) {
        form.setWorkshopId(id);
        workshopHorarioService.guardarSlot(form);
        redirectAttributes.addFlashAttribute("mensajeExito", "Horario del workshop actualizado.");
        return "redirect:/workshops/" + id + "/horario";
    }
}
