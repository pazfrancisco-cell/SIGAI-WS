package ar.edu.wellspring.sigaiws.modules.asistencia.web;

import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.asistencia.domain.AsistenciaJornada;
import ar.edu.wellspring.sigaiws.modules.asistencia.domain.TipoAsistenciaJornada;
import ar.edu.wellspring.sigaiws.modules.asistencia.dto.AsistenciaClaseBatchForm;
import ar.edu.wellspring.sigaiws.modules.asistencia.dto.AsistenciaJornadaBatchForm;
import ar.edu.wellspring.sigaiws.modules.asistencia.dto.JustificacionForm;
import ar.edu.wellspring.sigaiws.modules.asistencia.domain.JustificacionInasistencia;
import ar.edu.wellspring.sigaiws.modules.asistencia.repository.AsistenciaJornadaRepository;
import ar.edu.wellspring.sigaiws.modules.asistencia.repository.JustificacionInasistenciaRepository;
import ar.edu.wellspring.sigaiws.modules.asistencia.service.AsistenciaService;
import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.MateriaRepository;
import ar.edu.wellspring.sigaiws.modules.horario.repository.AsignacionDocenteRepository;
import ar.edu.wellspring.sigaiws.modules.horario.repository.HorarioSlotRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

/** Modulo 5.2. Pantallas 5.2.5 (preceptor), 5.2.6 (docente), 5.2.7 (historial), 5.2.8 (justificacion). */
@Controller
@RequiredArgsConstructor
public class AsistenciaController {

    private final AsistenciaService asistenciaService;
    private final AsistenciaJornadaRepository asistenciaJornadaRepository;
    private final JustificacionInasistenciaRepository justificacionInasistenciaRepository;
    private final CursoDivisionRepository cursoDivisionRepository;
    private final MateriaRepository materiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final HorarioSlotRepository horarioSlotRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final CicloLectivoRepository cicloLectivoRepository;

    @GetMapping("/asistencia/preceptor")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','PRECEPTOR')")
    public String tomaPreceptor(@RequestParam(required = false) Long cursoDivisionId,
                                 @RequestParam(required = false) String turno,
                                 @RequestParam(required = false) Integer anio,
                                 @RequestParam(required = false) Integer mes,
                                 @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fecha,
                                 Model model) {
        var divisiones = cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc();
        Long division = cursoDivisionId != null ? cursoDivisionId : (divisiones.isEmpty() ? null : divisiones.get(0).getId());
        AsistenciaJornada.Turno turnoSeleccionado = turno != null ? AsistenciaJornada.Turno.valueOf(turno) : AsistenciaJornada.Turno.MANANA;

        LocalDate hoy = LocalDate.now();
        LocalDate fechaSeleccionada = fecha != null ? fecha : hoy;
        if (fechaSeleccionada.isAfter(hoy)) {
            fechaSeleccionada = hoy; // no se puede tomar/editar asistencia de un dia futuro
        }

        model.addAttribute("cursosDivisiones", divisiones);
        model.addAttribute("divisionSeleccionada", division);
        model.addAttribute("turnoSeleccionado", turnoSeleccionado);
        model.addAttribute("fecha", fechaSeleccionada);
        model.addAttribute("hoy", hoy);

        int anioSel = anio != null ? anio : fechaSeleccionada.getYear();
        int mesSel = mes != null ? mes : fechaSeleccionada.getMonthValue();
        LocalDate primerDia = LocalDate.of(anioSel, mesSel, 1);
        model.addAttribute("anioSel", anioSel);
        model.addAttribute("mesSel", mesSel);
        model.addAttribute("semanas", construirSemanas(primerDia));

        Map<Long, TipoAsistenciaJornada> tipoPorAlumno = new HashMap<>();
        Set<Integer> diasConRegistro = new HashSet<>();
        if (division != null) {
            List<Alumno> alumnos = asistenciaService.alumnosDeDivision(division);
            model.addAttribute("alumnos", alumnos);
            List<Long> alumnoIds = alumnos.stream().map(Alumno::getId).toList();

            for (AsistenciaJornada r : asistenciaJornadaRepository
                    .findAllByAlumno_IdInAndFechaAndTurno(alumnoIds, fechaSeleccionada, turnoSeleccionado)) {
                tipoPorAlumno.put(r.getAlumno().getId(), r.getTipo());
            }

            LocalDate ultimoDiaMes = primerDia.withDayOfMonth(primerDia.lengthOfMonth());
            for (AsistenciaJornada r : asistenciaJornadaRepository
                    .findAllByAlumno_IdInAndFechaBetween(alumnoIds, primerDia, ultimoDiaMes)) {
                diasConRegistro.add(r.getFecha().getDayOfMonth());
            }
        }
        model.addAttribute("tipoPorAlumno", tipoPorAlumno);
        model.addAttribute("diasConRegistro", diasConRegistro);
        return "asistencia/preceptor";
    }

    @PostMapping("/asistencia/preceptor")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','PRECEPTOR')")
    public String guardarPreceptor(@ModelAttribute AsistenciaJornadaBatchForm form,
                                    @AuthenticationPrincipal SigaiUserPrincipal principal,
                                    RedirectAttributes redirectAttributes) {
        asistenciaService.registrarJornada(form, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Asistencia guardada correctamente.");
        return "redirect:/asistencia/preceptor?cursoDivisionId=" + form.getCursoDivisionId()
                + "&turno=" + form.getTurno() + "&fecha=" + form.getFecha();
    }

    @GetMapping("/asistencia/resumen")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','PRECEPTOR')")
    public String resumenPorCurso(@RequestParam(required = false) Long cursoDivisionId, Model model) {
        var divisiones = cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc();
        Long division = cursoDivisionId != null ? cursoDivisionId : (divisiones.isEmpty() ? null : divisiones.get(0).getId());

        model.addAttribute("cursosDivisiones", divisiones);
        model.addAttribute("divisionSeleccionada", division);
        if (division != null) {
            model.addAttribute("trayectorias", asistenciaService.resumenPorDivision(division));
        }
        return "asistencia/resumen";
    }

    @GetMapping("/asistencia/docente")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','PRECEPTOR','DOCENTE')")
    public String tomaDocente(@RequestParam(required = false) Long materiaId,
                               @RequestParam(required = false) Long cursoDivisionId,
                               @AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        if (principal.getUsuario().tieneRol(RolNombre.DOCENTE)) {
            var cicloActivo = cicloLectivoRepository.findByActivoTrue().orElse(null);
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
        model.addAttribute("fecha", LocalDate.now());
        if (materiaId != null && cursoDivisionId != null) {
            model.addAttribute("alumnos", asistenciaService.alumnosDeDivision(cursoDivisionId));
        }
        return "asistencia/docente";
    }

    @PostMapping("/asistencia/docente")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','PRECEPTOR','DOCENTE')")
    public String guardarDocente(@ModelAttribute AsistenciaClaseBatchForm form,
                                  @AuthenticationPrincipal SigaiUserPrincipal principal,
                                  RedirectAttributes redirectAttributes) {
        asistenciaService.registrarClase(form, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Asistencia de la clase guardada.");
        return "redirect:/asistencia/docente?materiaId=" + form.getMateriaId() + "&cursoDivisionId=" + form.getCursoDivisionId();
    }

    @GetMapping("/alumnos/{id}/asistencia")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','PRECEPTOR')")
    public String historial(@PathVariable Long id,
                             @RequestParam(required = false) Integer anio,
                             @RequestParam(required = false) Integer mes,
                             Model model) {
        var registros = asistenciaJornadaRepository.findAllByAlumno_IdOrderByFechaDesc(id);
        model.addAttribute("alumno", registros);
        model.addAttribute("alumnoId", id);
        model.addAttribute("registros", registros);

        Map<Long, JustificacionInasistencia> justificaciones = new HashMap<>();
        for (var registro : registros) {
            justificacionInasistenciaRepository.findByAsistenciaJornada_Id(registro.getId())
                    .ifPresent(j -> justificaciones.put(registro.getId(), j));
        }
        model.addAttribute("justificaciones", justificaciones);

        // Seccion 5.2.7: "Vista de calendario mensual: cada dia del mes con el estado
        // de asistencia representado por un color".
        java.time.LocalDate hoy = java.time.LocalDate.now();
        int anioSel = anio != null ? anio : hoy.getYear();
        int mesSel = mes != null ? mes : hoy.getMonthValue();
        java.time.LocalDate primerDia = java.time.LocalDate.of(anioSel, mesSel, 1);
        java.time.LocalDate ultimoDia = primerDia.withDayOfMonth(primerDia.lengthOfMonth());
        var registrosMes = asistenciaJornadaRepository.findAllByAlumno_IdAndFechaBetweenOrderByFecha(id, primerDia, ultimoDia);

        Map<Integer, TipoAsistenciaJornada> tipoPorDia = new HashMap<>();
        for (var r : registrosMes) {
            int dia = r.getFecha().getDayOfMonth();
            tipoPorDia.merge(dia, r.getTipo(), (actual, nuevo) -> rankGravedad(nuevo) > rankGravedad(actual) ? nuevo : actual);
        }

        model.addAttribute("anioSel", anioSel);
        model.addAttribute("mesSel", mesSel);
        model.addAttribute("semanas", construirSemanas(primerDia));
        model.addAttribute("tipoPorDia", tipoPorDia);
        return "asistencia/historial";
    }

    /** Grilla de semanas (Lunes a Domingo) del mes de {@code primerDia}; 0 = celda vacia fuera del mes. */
    private List<List<Integer>> construirSemanas(LocalDate primerDia) {
        List<List<Integer>> semanas = new ArrayList<>();
        List<Integer> semanaActual = new ArrayList<>();
        int offset = primerDia.getDayOfWeek().getValue() - 1; // 0=Lunes
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

    private int rankGravedad(TipoAsistenciaJornada tipo) {
        return switch (tipo) {
            case AUSENTE -> 5;
            case AUSENTE_JUSTIFICADO -> 4;
            case MEDIA_FALTA -> 3;
            case LLEGADA_TARDE, UNIFORME_INCOMPLETO -> 2;
            case PRESENTE -> 1;
        };
    }

    @PostMapping("/asistencia/justificar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','PRECEPTOR')")
    public String justificar(@ModelAttribute JustificacionForm form, @RequestParam Long alumnoId,
                              @AuthenticationPrincipal SigaiUserPrincipal principal,
                              RedirectAttributes redirectAttributes) {
        asistenciaService.justificar(form, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Inasistencia justificada.");
        return "redirect:/alumnos/" + alumnoId + "/asistencia";
    }
}
