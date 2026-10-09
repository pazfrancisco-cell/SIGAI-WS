package ar.edu.wellspring.sigaiws.modules.horario.web;

import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.AulaRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.MateriaRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.PeriodoHorarioRepository;
import ar.edu.wellspring.sigaiws.modules.horario.dto.SlotForm;
import ar.edu.wellspring.sigaiws.modules.horario.service.HorarioPdfGenerator;
import ar.edu.wellspring.sigaiws.modules.horario.service.HorarioSlotService;
import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import ar.edu.wellspring.sigaiws.modules.workshop.service.WorkshopHorarioService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Seccion 5.5.3/5.5.4: edicion Superadmin/Directivo/Administrativo, consulta
 * Docente/Preceptor. El grid semanal es de 5 dias x 5 periodos por division.
 */
@Controller
@RequestMapping("/horarios")
@PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','PRECEPTOR','DOCENTE')")
@RequiredArgsConstructor
public class HorarioController {

    private final HorarioSlotService horarioSlotService;
    private final WorkshopHorarioService workshopHorarioService;
    private final HorarioPdfGenerator horarioPdfGenerator;
    private final CursoDivisionRepository cursoDivisionRepository;
    private final MateriaRepository materiaRepository;
    private final AulaRepository aulaRepository;
    private final PeriodoHorarioRepository periodoHorarioRepository;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public String vistaGrid(java.util.Optional<Long> cursoDivisionId, Model model) {
        var divisiones = cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc();
        Long divisionSeleccionada = cursoDivisionId.orElseGet(() -> divisiones.isEmpty() ? null : divisiones.get(0).getId());

        model.addAttribute("cursosDivisiones", divisiones);
        model.addAttribute("divisionSeleccionada", divisionSeleccionada);
        model.addAttribute("periodos", periodoHorarioRepository.findAllByTipoOrderByOrden(
                ar.edu.wellspring.sigaiws.modules.configuracion.domain.PeriodoHorario.Tipo.CLASE));
        model.addAttribute("materias", materiaRepository.findAllByActivaTrueOrderByNombre());
        model.addAttribute("aulas", aulaRepository.findAllByActivaTrueOrderByNombre());
        model.addAttribute("docentes", usuarioRepository.buscar(null, true, null, PageRequest.of(0, 200)).getContent()
                .stream().filter(u -> u.tieneRol(RolNombre.DOCENTE)).toList());

        if (divisionSeleccionada != null) {
            model.addAttribute("slotsPorClave", mapaPorClave(horarioSlotService.gridDeDivision(divisionSeleccionada)));
        }
        return "horarios/grid";
    }

    /**
     * Clave "dia-periodo" -> slot. Evita el selector SpEL ".?[...]" en el
     * template: cuando la coleccion tiene al menos un elemento, esa sintaxis
     * intenta resolver las variables del th:each externo (ej. "dia") como
     * propiedad del elemento actual de la seleccion en vez de como variable
     * de contexto, y falla con SpelEvaluationException.
     */
    private Map<String, ar.edu.wellspring.sigaiws.modules.horario.domain.HorarioSlot> mapaPorClave(
            java.util.List<ar.edu.wellspring.sigaiws.modules.horario.domain.HorarioSlot> slots) {
        Map<String, ar.edu.wellspring.sigaiws.modules.horario.domain.HorarioSlot> mapa = new java.util.HashMap<>();
        for (var slot : slots) {
            mapa.put(slot.getDiaSemana() + "-" + slot.getPeriodoOrden(), slot);
        }
        return mapa;
    }

    @PostMapping("/slot")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String guardarSlot(@ModelAttribute SlotForm slotForm, HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        horarioSlotService.guardarSlot(slotForm);
        redirectAttributes.addFlashAttribute("mensajeExito", "Horario actualizado.");
        return "redirect:/horarios?cursoDivisionId=" + slotForm.getCursoDivisionId();
    }

    @GetMapping("/mi-horario")
    @PreAuthorize("hasRole('DOCENTE')")
    public String miHorario(@AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        model.addAttribute("slotsPorClave", mapaPorClave(horarioSlotService.miHorario(principal.getId())));
        model.addAttribute("workshopSlotsPorClave", mapaWorkshopPorClave(workshopHorarioService.miHorario(principal.getId())));
        model.addAttribute("periodos", periodoHorarioRepository.findAllByTipoOrderByOrden(
                ar.edu.wellspring.sigaiws.modules.configuracion.domain.PeriodoHorario.Tipo.CLASE));
        return "horarios/mi-horario";
    }

    private Map<String, ar.edu.wellspring.sigaiws.modules.workshop.domain.WorkshopHorarioSlot> mapaWorkshopPorClave(
            java.util.List<ar.edu.wellspring.sigaiws.modules.workshop.domain.WorkshopHorarioSlot> slots) {
        Map<String, ar.edu.wellspring.sigaiws.modules.workshop.domain.WorkshopHorarioSlot> mapa = new java.util.HashMap<>();
        for (var slot : slots) {
            mapa.put(slot.getDiaSemana() + "-" + slot.getPeriodoOrden(), slot);
        }
        return mapa;
    }

    @GetMapping("/mi-horario/pdf")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<byte[]> miHorarioPdf(@AuthenticationPrincipal SigaiUserPrincipal principal) {
        var periodos = periodoHorarioRepository.findAllByTipoOrderByOrden(
                ar.edu.wellspring.sigaiws.modules.configuracion.domain.PeriodoHorario.Tipo.CLASE);
        var slots = horarioSlotService.miHorario(principal.getId());
        byte[] pdf = horarioPdfGenerator.generarMiHorario(principal.getUsuario(), periodos, slots);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"mi_horario.pdf\"")
                .body(pdf);
    }

    /** Seccion 5.5.3: "Vista Global que muestra un resumen de todos los horarios en una sola pantalla (solo lectura)". */
    @GetMapping("/vista-global")
    public String vistaGlobal(Model model) {
        var divisiones = cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc();
        var periodos = periodoHorarioRepository.findAllByTipoOrderByOrden(
                ar.edu.wellspring.sigaiws.modules.configuracion.domain.PeriodoHorario.Tipo.CLASE);
        Map<Long, Map<String, ar.edu.wellspring.sigaiws.modules.horario.domain.HorarioSlot>> slotsPorDivision = new LinkedHashMap<>();
        for (var division : divisiones) {
            slotsPorDivision.put(division.getId(), mapaPorClave(horarioSlotService.gridDeDivision(division.getId())));
        }
        model.addAttribute("cursosDivisiones", divisiones);
        model.addAttribute("periodos", periodos);
        model.addAttribute("slotsPorDivision", slotsPorDivision);
        return "horarios/vista-global";
    }
}
