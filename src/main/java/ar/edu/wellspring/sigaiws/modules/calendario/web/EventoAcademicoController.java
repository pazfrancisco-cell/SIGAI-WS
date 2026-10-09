package ar.edu.wellspring.sigaiws.modules.calendario.web;

import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.calendario.domain.EventoAcademico;
import ar.edu.wellspring.sigaiws.modules.calendario.dto.EventoAcademicoForm;
import ar.edu.wellspring.sigaiws.modules.calendario.service.EventoAcademicoService;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Calendario academico: visible para todos los roles autenticados, carga solo para admin/directivo. */
@Controller
@RequestMapping("/calendario")
@RequiredArgsConstructor
public class EventoAcademicoController {

    private final EventoAcademicoService eventoAcademicoService;
    private final CursoDivisionRepository cursoDivisionRepository;

    @GetMapping
    public String vista(@RequestParam(required = false) Integer anio, @RequestParam(required = false) Integer mes,
                         @AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        var usuario = principal.getUsuario();
        List<EventoAcademico> eventos;
        if (usuario.tieneRol(RolNombre.SUPERADMIN) || usuario.tieneRol(RolNombre.DIRECTIVO)
                || usuario.tieneRol(RolNombre.ADMINISTRATIVO) || usuario.tieneRol(RolNombre.PRECEPTOR)) {
            eventos = eventoAcademicoService.listarTodos();
        } else if (usuario.tieneRol(RolNombre.DOCENTE)) {
            eventos = eventoAcademicoService.listarParaDocente(principal.getId());
        } else if (usuario.tieneRol(RolNombre.PADRE_TUTOR)) {
            eventos = eventoAcademicoService.listarParaTutor(principal.getId());
        } else {
            eventos = List.of();
        }

        LocalDate hoy = LocalDate.now();
        int anioSel = anio != null ? anio : hoy.getYear();
        int mesSel = mes != null ? mes : hoy.getMonthValue();
        LocalDate primerDia = LocalDate.of(anioSel, mesSel, 1);
        LocalDate ultimoDia = primerDia.withDayOfMonth(primerDia.lengthOfMonth());

        List<EventoAcademico> eventosDelMes = eventos.stream()
                .filter(e -> !e.getFechaInicio().isAfter(ultimoDia) && !e.getFechaFinEfectiva().isBefore(primerDia))
                .toList();

        Map<Integer, List<EventoAcademico>> eventosPorDia = new HashMap<>();
        for (var evento : eventosDelMes) {
            LocalDate desde = evento.getFechaInicio().isBefore(primerDia) ? primerDia : evento.getFechaInicio();
            LocalDate hasta = evento.getFechaFinEfectiva().isAfter(ultimoDia) ? ultimoDia : evento.getFechaFinEfectiva();
            for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
                eventosPorDia.computeIfAbsent(d.getDayOfMonth(), k -> new ArrayList<>()).add(evento);
            }
        }

        boolean puedeAdministrar = usuario.tieneRol(RolNombre.SUPERADMIN) || usuario.tieneRol(RolNombre.DIRECTIVO)
                || usuario.tieneRol(RolNombre.ADMINISTRATIVO);
        model.addAttribute("puedeAdministrar", puedeAdministrar);
        model.addAttribute("eventos", eventos);
        model.addAttribute("eventosDelMes", eventosDelMes);
        model.addAttribute("eventosPorDia", eventosPorDia);
        model.addAttribute("anioSel", anioSel);
        model.addAttribute("mesSel", mesSel);
        model.addAttribute("semanas", construirSemanas(primerDia));
        return "calendario/vista";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String nuevoForm(Model model) {
        model.addAttribute("eventoForm", new EventoAcademicoForm());
        model.addAttribute("cursosDivisiones", cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc());
        return "calendario/form";
    }

    @GetMapping("/{id}/editar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String editarForm(@PathVariable Long id, Model model) {
        var evento = eventoAcademicoService.obtener(id);
        EventoAcademicoForm form = new EventoAcademicoForm();
        form.setId(evento.getId());
        form.setTitulo(evento.getTitulo());
        form.setDescripcion(evento.getDescripcion());
        form.setTipo(evento.getTipo());
        form.setFechaInicio(evento.getFechaInicio());
        form.setFechaFin(evento.getFechaFin());
        form.setCursoDivisionId(evento.getCursoDivision() != null ? evento.getCursoDivision().getId() : null);
        model.addAttribute("eventoForm", form);
        model.addAttribute("cursosDivisiones", cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc());
        return "calendario/form";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String crear(@Valid @ModelAttribute("eventoForm") EventoAcademicoForm form, BindingResult bindingResult,
                         @AuthenticationPrincipal SigaiUserPrincipal principal, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("cursosDivisiones", cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc());
            return "calendario/form";
        }
        eventoAcademicoService.crear(form, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Evento creado.");
        return "redirect:/calendario";
    }

    @PostMapping("/{id}/editar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String editar(@PathVariable Long id, @Valid @ModelAttribute("eventoForm") EventoAcademicoForm form,
                          BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("cursosDivisiones", cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc());
            return "calendario/form";
        }
        eventoAcademicoService.editar(id, form);
        redirectAttributes.addFlashAttribute("mensajeExito", "Evento actualizado.");
        return "redirect:/calendario";
    }

    @PostMapping("/{id}/eliminar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        eventoAcademicoService.eliminar(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Evento eliminado.");
        return "redirect:/calendario";
    }

    /** Grilla de semanas (Lunes a Domingo) del mes de {@code primerDia}; 0 = celda vacia fuera del mes. */
    private List<List<Integer>> construirSemanas(LocalDate primerDia) {
        List<List<Integer>> semanas = new ArrayList<>();
        List<Integer> semanaActual = new ArrayList<>();
        int offset = primerDia.getDayOfWeek().getValue() - 1;
        for (int i = 0; i < offset; i++) semanaActual.add(0);
        for (int dia = 1; dia <= primerDia.lengthOfMonth(); dia++) {
            semanaActual.add(dia);
            if (semanaActual.size() == 7) {
                semanas.add(semanaActual);
                semanaActual = new ArrayList<>();
            }
        }
        if (!semanaActual.isEmpty()) {
            while (semanaActual.size() < 7) semanaActual.add(0);
            semanas.add(semanaActual);
        }
        return semanas;
    }
}
