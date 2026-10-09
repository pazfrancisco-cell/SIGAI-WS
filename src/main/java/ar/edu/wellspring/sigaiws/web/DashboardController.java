package ar.edu.wellspring.sigaiws.web;

import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.modules.alerta.service.AlertaService;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoCicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.TutorRepository;
import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.calificacion.service.CalificacionService;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.edfisica.service.AptoFisicoService;
import ar.edu.wellspring.sigaiws.modules.edfisica.service.EducacionFisicaService;
import ar.edu.wellspring.sigaiws.modules.horario.repository.HorarioSlotRepository;
import ar.edu.wellspring.sigaiws.modules.mensajeria.service.ComunicadoService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Seccion 5.9 (Dashboards Institucionales). "/" enruta segun el rol activo
 * del usuario, mostrando los widgets mas relevantes para su funcion.
 */
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final AlumnoCicloLectivoRepository alumnoCicloLectivoRepository;
    private final AlertaService alertaService;
    private final CalificacionService calificacionService;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final HorarioSlotRepository horarioSlotRepository;
    private final TutorRepository tutorRepository;
    private final ComunicadoService comunicadoService;
    private final EducacionFisicaService educacionFisicaService;
    private final AptoFisicoService aptoFisicoService;
    private final DashboardService dashboardService;
    private final ar.edu.wellspring.sigaiws.modules.alumno.service.PromocionService promocionService;

    @GetMapping("/")
    public String dashboard(@AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        var usuario = principal.getUsuario();

        if (usuario.tieneRol(RolNombre.SUPERADMIN) || usuario.tieneRol(RolNombre.DIRECTIVO) || usuario.tieneRol(RolNombre.ADMINISTRATIVO)) {
            var cicloActivo = cicloLectivoRepository.findByActivoTrue().orElse(null);
            long enCondicionLibre = cicloActivo == null ? 0 : alumnoCicloLectivoRepository.findAllByCicloLectivo_Id(cicloActivo.getId())
                    .stream().filter(acl -> acl.isCondicionLibre()).count();
            model.addAttribute("enCondicionLibre", enCondicionLibre);
            model.addAttribute("alertasActivas", alertaService.activas().size());
            model.addAttribute("solicitudesPendientes", calificacionService.solicitudesPendientes().size());
            model.addAttribute("alumnosSinCurso", promocionService.alumnosSinCursoEnCicloActivo());

            var comunicadosRecientes = comunicadoService.listar().stream().limit(5).toList();
            model.addAttribute("comunicadosRecientes", comunicadosRecientes);
            Map<Long, ComunicadoService.ResumenLectura> resumenesComunicados = new HashMap<>();
            for (var c : comunicadosRecientes) {
                resumenesComunicados.put(c.getId(), comunicadoService.resumenLectura(c.getId()));
            }
            model.addAttribute("resumenesComunicados", resumenesComunicados);

            if (cicloActivo != null) {
                model.addAttribute("asistenciaInstitucional", dashboardService.asistenciaInstitucional(cicloActivo));
                model.addAttribute("countdownCuatrimestre", dashboardService.countdownCierreCuatrimestre(cicloActivo));
                model.addAttribute("alumnosProximosAlUmbral", dashboardService.alumnosProximosAlUmbral(cicloActivo));
            }
            model.addAttribute("metricasDisciplinarias", dashboardService.disciplinaUltimos7Dias());
            return "dashboard/directivo";
        }

        if (usuario.tieneRol(RolNombre.PRECEPTOR)) {
            var todasLasAlertas = alertaService.activas();
            model.addAttribute("alertasActivas", todasLasAlertas.stream().limit(5).toList());
            model.addAttribute("totalAlertasActivas", todasLasAlertas.size());
            var cicloActivo = cicloLectivoRepository.findByActivoTrue().orElse(null);
            if (cicloActivo != null) {
                model.addAttribute("alumnosProximosAlUmbral", dashboardService.alumnosProximosAlUmbral(cicloActivo));
            }
            model.addAttribute("metricasDisciplinarias", dashboardService.disciplinaUltimos7Dias());
            return "dashboard/preceptor";
        }

        if (usuario.tieneRol(RolNombre.DOCENTE)) {
            int diaHoy = LocalDate.now().getDayOfWeek().getValue();
            var cicloActivo = cicloLectivoRepository.findByActivoTrue().orElse(null);
            var horarioHoy = cicloActivo == null || diaHoy > DayOfWeek.FRIDAY.getValue() ? java.util.List.of()
                    : horarioSlotRepository.findAllByDocente_IdAndCicloLectivo_Id(principal.getId(), cicloActivo.getId())
                        .stream().filter(s -> s.getDiaSemana() == diaHoy).sorted(java.util.Comparator.comparingInt(s -> s.getPeriodoOrden())).toList();
            model.addAttribute("horarioHoy", horarioHoy);

            if (cicloActivo != null) {
                model.addAttribute("countdownCuatrimestre", dashboardService.countdownCierreCuatrimestre(cicloActivo));
                model.addAttribute("evaluacionesSinCerrar", dashboardService.evaluacionesSinCerrar(principal.getId(), cicloActivo));
            }

            if (usuario.isEsDocenteEducacionFisica()) {
                List<ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno> pendientesAptoFisico = new ArrayList<>();
                java.util.Set<Long> vistos = new java.util.HashSet<>();
                for (var grupo : educacionFisicaService.gruposDelDocente(principal.getId())) {
                    for (int cuatrimestre = 1; cuatrimestre <= 2; cuatrimestre++) {
                        for (var alumno : educacionFisicaService.alumnosDelGrupo(grupo.getId(), cuatrimestre)) {
                            if (vistos.add(alumno.getId()) && aptoFisicoService.esSaf(alumno.getId())) {
                                pendientesAptoFisico.add(alumno);
                            }
                        }
                    }
                }
                model.addAttribute("pendientesAptoFisico", pendientesAptoFisico);
            }
            return "dashboard/docente";
        }

        if (usuario.tieneRol(RolNombre.PADRE_TUTOR)) {
            var tutor = tutorRepository.findAll().stream().filter(t -> t.getUsuarioId() != null && t.getUsuarioId().equals(principal.getId())).findFirst();
            model.addAttribute("tutor", tutor.orElse(null));
            return "redirect:/portal-padres";
        }

        return "redirect:/alumnos";
    }
}
