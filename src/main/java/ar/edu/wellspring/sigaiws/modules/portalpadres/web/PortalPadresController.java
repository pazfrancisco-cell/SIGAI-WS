package ar.edu.wellspring.sigaiws.modules.portalpadres.web;

import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoTutorRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.service.AlumnoService;
import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.disciplina.service.DisciplinaService;
import ar.edu.wellspring.sigaiws.modules.horario.repository.HorarioSlotRepository;
import ar.edu.wellspring.sigaiws.modules.mensajeria.service.ComunicadoService;
import ar.edu.wellspring.sigaiws.modules.portalpadres.service.PortalPadresService;
import ar.edu.wellspring.sigaiws.modules.reporte.service.ReporteService;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Modulo 5.11 (Portal de Padres/Tutores). Todo de solo lectura; se verifica en
 * cada request que el alumno consultado este vinculado al tutor autenticado
 * (seccion 5.11.3: "no puede... acceder a informacion de otros alumnos que no
 * esten vinculados a su cuenta").
 */
@Controller
@RequestMapping("/portal-padres")
@PreAuthorize("hasRole('PADRE_TUTOR')")
@RequiredArgsConstructor
public class PortalPadresController {

    private final AlumnoTutorRepository alumnoTutorRepository;
    private final AlumnoService alumnoService;
    private final PortalPadresService portalPadresService;
    private final DisciplinaService disciplinaService;
    private final HorarioSlotRepository horarioSlotRepository;
    private final ReporteService reporteService;
    private final ComunicadoService comunicadoService;
    private final ar.edu.wellspring.sigaiws.modules.calificacion.service.MesaExamenService mesaExamenService;

    @GetMapping
    public String dashboard(@AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        var vinculos = alumnoTutorRepository.findAllByTutor_UsuarioId(principal.getId());
        model.addAttribute("hijos", vinculos.stream().map(v -> v.getAlumno()).toList());

        var comunicados = comunicadoService.comunicadosParaPadre(principal.getId()).stream().limit(5).toList();
        Set<Long> leidos = new HashSet<>();
        for (var c : comunicados) {
            comunicadoService.lecturasDe(c.getId()).stream()
                    .filter(l -> l.getUsuario().getId().equals(principal.getId()))
                    .findAny().ifPresent(l -> leidos.add(c.getId()));
        }
        model.addAttribute("comunicadosRecientes", comunicados);
        model.addAttribute("comunicadosLeidosIds", leidos);
        return "portalpadres/dashboard";
    }

    @GetMapping("/comunicados")
    public String comunicados(@AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        var comunicados = comunicadoService.comunicadosParaPadre(principal.getId());
        Set<Long> leidos = new HashSet<>();
        for (var c : comunicados) {
            comunicadoService.lecturasDe(c.getId()).stream()
                    .filter(l -> l.getUsuario().getId().equals(principal.getId()))
                    .findAny().ifPresent(l -> leidos.add(c.getId()));
        }
        model.addAttribute("comunicados", comunicados);
        model.addAttribute("leidosIds", leidos);
        return "portalpadres/comunicados";
    }

    @GetMapping("/comunicados/{id}")
    public String comunicadoDetalle(@PathVariable Long id, @AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        if (!comunicadoService.esVisibleParaPadre(id, principal.getId())) {
            throw new BusinessException("Ese comunicado no esta dirigido a tu cuenta.");
        }
        comunicadoService.marcarLeido(id, principal.getId());
        model.addAttribute("comunicado", comunicadoService.obtener(id));
        return "portalpadres/comunicado-detalle";
    }

    @GetMapping("/alumno/{id}")
    public String detalle(@PathVariable Long id, @AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        validarAcceso(id, principal.getId());

        var alumno = alumnoService.obtener(id);
        model.addAttribute("alumno", alumno);
        try {
            var cicloActual = alumnoService.cicloActualDe(id);
            model.addAttribute("cicloActual", cicloActual);
            model.addAttribute("calificaciones", portalPadresService.calificacionesDetalladas(id, cicloActual));
            model.addAttribute("diasConFalta", portalPadresService.diasConFalta(id, cicloActual.getCicloLectivo()));
            model.addAttribute("horario", horarioSlotRepository.findAllByCursoDivision_IdAndCicloLectivo_Id(
                    cicloActual.getCursoDivision().getId(), cicloActual.getCicloLectivo().getId()));
        } catch (RuntimeException ignored) {
            // sin trayectoria en el ciclo activo
        }
        model.addAttribute("materiasAdeudadas", mesaExamenService.deAlumno(id));
        model.addAttribute("observaciones", disciplinaService.historialDe(id));
        model.addAttribute("reportes", reporteService.reportesDe(id));
        return "portalpadres/detalle";
    }

    private void validarAcceso(Long alumnoId, Long usuarioId) {
        boolean vinculado = alumnoTutorRepository.findAllByTutor_UsuarioId(usuarioId).stream()
                .anyMatch(v -> v.getAlumno().getId().equals(alumnoId));
        if (!vinculado) {
            throw new BusinessException("No tenes acceso a la informacion de ese alumno.");
        }
    }
}
