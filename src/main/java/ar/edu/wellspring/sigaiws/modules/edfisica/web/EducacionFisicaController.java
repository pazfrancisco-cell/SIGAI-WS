package ar.edu.wellspring.sigaiws.modules.edfisica.web;

import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoRepository;
import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.edfisica.domain.GrupoEducacionFisica;
import ar.edu.wellspring.sigaiws.modules.edfisica.dto.GrupoForm;
import ar.edu.wellspring.sigaiws.modules.edfisica.dto.PruebaFisicaDefinicionForm;
import ar.edu.wellspring.sigaiws.modules.edfisica.dto.PruebaFisicaForm;
import ar.edu.wellspring.sigaiws.modules.edfisica.dto.AsistenciaEducacionFisicaBatchForm;
import ar.edu.wellspring.sigaiws.modules.edfisica.dto.RestriccionMedicaForm;
import ar.edu.wellspring.sigaiws.modules.edfisica.service.AptoFisicoService;
import ar.edu.wellspring.sigaiws.modules.edfisica.service.AsistenciaEducacionFisicaService;
import ar.edu.wellspring.sigaiws.modules.edfisica.service.EducacionFisicaService;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Modulo 5.7. */
@Controller
@RequestMapping("/educacion-fisica")
@RequiredArgsConstructor
public class EducacionFisicaController {

    private final EducacionFisicaService educacionFisicaService;
    private final AptoFisicoService aptoFisicoService;
    private final AsistenciaEducacionFisicaService asistenciaEducacionFisicaService;
    private final AlumnoRepository alumnoRepository;
    private final UsuarioRepository usuarioRepository;

    /** Seccion 5.7 (extension): toma de asistencia diaria del grupo, con SAF como estado (cuenta como presente). */
    @GetMapping("/asistencia")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO') or (hasRole('DOCENTE') and principal.usuario.esDocenteEducacionFisica)")
    public String asistencia(@RequestParam(required = false) Long grupoId,
                              @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fecha,
                              @AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        LocalDate fechaEfectiva = fecha != null ? fecha : LocalDate.now();
        var grupos = principal.getUsuario().tieneRol(RolNombre.DOCENTE)
                ? educacionFisicaService.gruposDelDocente(principal.getId())
                : educacionFisicaService.grupos();
        model.addAttribute("grupos", grupos);
        model.addAttribute("grupoSeleccionado", grupoId);
        model.addAttribute("fecha", fechaEfectiva);
        if (grupoId != null) {
            var grupo = educacionFisicaService.obtenerGrupo(grupoId);
            if (principal.getUsuario().tieneRol(RolNombre.DOCENTE) && !grupo.getDocente().getId().equals(principal.getId())) {
                throw new AccessDeniedException("No estas a cargo de ese grupo.");
            }
            model.addAttribute("alumnos", educacionFisicaService.alumnosDelGrupo(grupoId, educacionFisicaService.cuatrimestreEnCurso()));
            model.addAttribute("estadoActual", asistenciaEducacionFisicaService.estadoDelDia(grupoId, fechaEfectiva));
        }
        return "edfisica/asistencia";
    }

    @PostMapping("/asistencia")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO') or (hasRole('DOCENTE') and principal.usuario.esDocenteEducacionFisica)")
    public String guardarAsistencia(@ModelAttribute AsistenciaEducacionFisicaBatchForm form,
                                     @AuthenticationPrincipal SigaiUserPrincipal principal,
                                     RedirectAttributes redirectAttributes) {
        var grupo = educacionFisicaService.obtenerGrupo(form.getGrupoId());
        if (principal.getUsuario().tieneRol(RolNombre.DOCENTE) && !grupo.getDocente().getId().equals(principal.getId())) {
            throw new AccessDeniedException("No estas a cargo de ese grupo.");
        }
        asistenciaEducacionFisicaService.registrar(form, grupo, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Asistencia registrada.");
        return "redirect:/educacion-fisica/asistencia?grupoId=" + form.getGrupoId() + "&fecha=" + form.getFecha();
    }

    @GetMapping("/grupos")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO') or (hasRole('DOCENTE') and principal.usuario.esDocenteEducacionFisica)")
    public String grupos(@AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        var grupos = principal.getUsuario().tieneRol(RolNombre.DOCENTE)
                ? educacionFisicaService.gruposDelDocente(principal.getId())
                : educacionFisicaService.grupos();
        model.addAttribute("grupos", grupos);
        model.addAttribute("resumenAptoFisico", aptoFisicoService.resumenCicloActivo());
        return "edfisica/grupos";
    }

    /** Disparo manual del chequeo diario de aptos fisicos vencidos (ademas del @Scheduled automatico). */
    @PostMapping("/apto-fisico/verificar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String verificarAptosFisicos(RedirectAttributes redirectAttributes) {
        aptoFisicoService.verificarVencimientos();
        redirectAttributes.addFlashAttribute("mensajeExito", "Verificacion de aptos fisicos ejecutada.");
        return "redirect:/educacion-fisica/grupos";
    }

    @GetMapping("/catalogo-pruebas")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String catalogoPruebas(Model model) {
        model.addAttribute("catalogo", educacionFisicaService.catalogoConUso());
        return "edfisica/catalogo-pruebas";
    }

    /** Fusiona dos pruebas del catalogo que resultaron ser la misma actividad con nombres distintos (seccion 5.7.4). */
    @PostMapping("/catalogo-pruebas/fusionar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String fusionarDefiniciones(@RequestParam Long descartarId, @RequestParam Long conservarId,
                                        RedirectAttributes redirectAttributes) {
        if (descartarId.equals(conservarId)) {
            redirectAttributes.addFlashAttribute("errorMensaje", "Elegi dos pruebas distintas para fusionar.");
        } else {
            educacionFisicaService.fusionarDefiniciones(descartarId, conservarId);
            redirectAttributes.addFlashAttribute("mensajeExito", "Pruebas fusionadas correctamente.");
        }
        return "redirect:/educacion-fisica/catalogo-pruebas";
    }

    @GetMapping("/grupos/nuevo")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO') or (hasRole('DOCENTE') and principal.usuario.esDocenteEducacionFisica)")
    public String nuevoGrupo(@RequestParam(required = false) Integer cuatrimestre,
                              @AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        GrupoForm form = new GrupoForm();
        form.setCuatrimestre(cuatrimestre != null ? cuatrimestre : educacionFisicaService.cuatrimestreEnCurso());
        if (principal.getUsuario().tieneRol(RolNombre.DOCENTE)) {
            form.setDocenteId(principal.getId());
        }
        prepararFormularioGrupo(model, form, List.of(), principal);
        return "edfisica/grupo-form";
    }

    @GetMapping("/grupos/{id}/editar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO') or (hasRole('DOCENTE') and principal.usuario.esDocenteEducacionFisica)")
    public String editarGrupo(@PathVariable Long id, @RequestParam(value = "cuatrimestre", required = false) Integer cuatrimestreParam,
                               @AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        GrupoEducacionFisica grupo = educacionFisicaService.obtenerGrupo(id);
        if (principal.getUsuario().tieneRol(RolNombre.DOCENTE) && !grupo.getDocente().getId().equals(principal.getId())) {
            throw new AccessDeniedException("No estas a cargo de ese grupo.");
        }
        int cuatrimestre = cuatrimestreParam != null ? cuatrimestreParam : educacionFisicaService.cuatrimestreEnCurso();
        GrupoForm form = new GrupoForm();
        form.setId(grupo.getId());
        form.setNombre(grupo.getNombre());
        form.setGenero(grupo.getGenero());
        form.setDocenteId(grupo.getDocente().getId());
        form.setCuatrimestre(cuatrimestre);
        form.setDefinicionIds(grupo.getPruebasDefiniciones().stream().map(d -> d.getId()).toList());
        var alumnosActuales = educacionFisicaService.alumnosDelGrupo(id, cuatrimestre);
        form.setAlumnoIds(alumnosActuales.stream().map(a -> a.getId()).toList());
        prepararFormularioGrupo(model, form, form.getAlumnoIds(), principal);
        return "edfisica/grupo-form";
    }

    private void prepararFormularioGrupo(Model model, GrupoForm form, List<Long> alumnoIdsActuales, SigaiUserPrincipal principal) {
        model.addAttribute("grupoForm", form);
        model.addAttribute("alumnoIdsActuales", alumnoIdsActuales);
        model.addAttribute("alumnos", educacionFisicaService.alumnosDisponiblesParaGrupo());
        model.addAttribute("definicionesDisponibles", educacionFisicaService.definicionesActivas());
        if (!principal.getUsuario().tieneRol(RolNombre.DOCENTE)) {
            model.addAttribute("docentes", usuarioRepository.buscar(null, true, null, PageRequest.of(0, 200)).getContent()
                    .stream().filter(u -> u.tieneRol(RolNombre.DOCENTE) && u.isEsDocenteEducacionFisica()).toList());
        }
    }

    @PostMapping("/grupos")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO') or (hasRole('DOCENTE') and principal.usuario.esDocenteEducacionFisica)")
    public String guardarGrupo(@Valid @ModelAttribute("grupoForm") GrupoForm form, BindingResult bindingResult,
                                @AuthenticationPrincipal SigaiUserPrincipal principal,
                                Model model, RedirectAttributes redirectAttributes) {
        if (principal.getUsuario().tieneRol(RolNombre.DOCENTE)) {
            if (form.getId() != null) {
                var existente = educacionFisicaService.obtenerGrupo(form.getId());
                if (!existente.getDocente().getId().equals(principal.getId())) {
                    throw new AccessDeniedException("No estas a cargo de ese grupo.");
                }
            }
            // El docente no puede reasignar el grupo a otro colega, sin importar lo que llegue en el formulario.
            form.setDocenteId(principal.getId());
        }
        if (bindingResult.hasErrors()) {
            prepararFormularioGrupo(model, form, form.getAlumnoIds(), principal);
            return "edfisica/grupo-form";
        }
        educacionFisicaService.guardarGrupo(form, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", form.getId() == null ? "Grupo creado." : "Grupo actualizado.");
        return "redirect:/educacion-fisica/grupos";
    }

    @GetMapping("/pruebas")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO') or (hasRole('DOCENTE') and principal.usuario.esDocenteEducacionFisica)")
    public String pruebas(@RequestParam(required = false) Long grupoId,
                           @RequestParam(required = false) Integer cuatrimestre,
                           @AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        int cuatrimestreEfectivo = cuatrimestre != null ? cuatrimestre : educacionFisicaService.cuatrimestreEnCurso();
        var grupos = principal.getUsuario().tieneRol(RolNombre.DOCENTE)
                ? educacionFisicaService.gruposDelDocente(principal.getId())
                : educacionFisicaService.grupos();
        model.addAttribute("grupos", grupos);
        model.addAttribute("grupoSeleccionado", grupoId);
        model.addAttribute("cuatrimestre", cuatrimestreEfectivo);
        model.addAttribute("resumenAptoFisico", aptoFisicoService.resumenCicloActivo());
        if (grupoId != null) {
            var todosLosAlumnos = educacionFisicaService.alumnosDelGrupo(grupoId, cuatrimestreEfectivo);
            List<ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno> aptos = new ArrayList<>();
            List<ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno> safAnual = new ArrayList<>();
            for (var alumno : todosLosAlumnos) {
                (aptoFisicoService.esSaf(alumno.getId()) ? safAnual : aptos).add(alumno);
            }
            model.addAttribute("alumnos", aptos);
            model.addAttribute("alumnosSafAnual", safAnual);
            model.addAttribute("pruebas", educacionFisicaService.pruebasDe(grupoId, cuatrimestreEfectivo));
            model.addAttribute("definicionesGrupo", educacionFisicaService.definicionesDelGrupo(grupoId));
        }
        return "edfisica/pruebas";
    }

    @PostMapping("/pruebas")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO') or (hasRole('DOCENTE') and principal.usuario.esDocenteEducacionFisica)")
    public String crearPrueba(@Valid @ModelAttribute PruebaFisicaForm form, BindingResult bindingResult,
                               @AuthenticationPrincipal SigaiUserPrincipal principal,
                               RedirectAttributes redirectAttributes) {
        String destino = "redirect:/educacion-fisica/pruebas?grupoId=" + form.getGrupoId() + "&cuatrimestre=" + form.getCuatrimestre();
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMensaje", "Elegi la prueba fisica y una fecha valida antes de guardar.");
            return destino;
        }
        educacionFisicaService.crearPrueba(form, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Prueba fisica registrada.");
        return destino;
    }

    /** Baja de una sesion de prueba ya cargada, por error o duplicacion (seccion 5.7.4). Borra sus resultados junto con ella. */
    @PostMapping("/pruebas/{id}/eliminar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO') or (hasRole('DOCENTE') and principal.usuario.esDocenteEducacionFisica)")
    public String eliminarPrueba(@PathVariable Long id, @RequestParam Long grupoId, @RequestParam int cuatrimestre,
                                  RedirectAttributes redirectAttributes) {
        educacionFisicaService.eliminarPrueba(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Prueba fisica eliminada.");
        return "redirect:/educacion-fisica/pruebas?grupoId=" + grupoId + "&cuatrimestre=" + cuatrimestre;
    }

    /** Alta rapida de una prueba nueva en el catalogo desde la pantalla de registro (seccion 5.7.4), quedando asignada al grupo actual. */
    @PostMapping("/pruebas/definiciones")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO') or (hasRole('DOCENTE') and principal.usuario.esDocenteEducacionFisica)")
    public String crearDefinicionRapida(@ModelAttribute PruebaFisicaDefinicionForm form,
                                         @RequestParam Long grupoId, @RequestParam int cuatrimestre,
                                         @AuthenticationPrincipal SigaiUserPrincipal principal,
                                         RedirectAttributes redirectAttributes) {
        var definicion = educacionFisicaService.obtenerOCrearDefinicion(form, principal.getUsuario());
        educacionFisicaService.agregarDefinicionAlGrupo(grupoId, definicion.getId());
        redirectAttributes.addFlashAttribute("mensajeExito", "Prueba fisica agregada al grupo.");
        return "redirect:/educacion-fisica/pruebas?grupoId=" + grupoId + "&cuatrimestre=" + cuatrimestre;
    }

    @GetMapping("/pruebas/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO') or (hasRole('DOCENTE') and principal.usuario.esDocenteEducacionFisica)")
    public String verRanking(@PathVariable Long id, Model model) {
        model.addAttribute("prueba", educacionFisicaService.obtenerPrueba(id));
        model.addAttribute("ranking", educacionFisicaService.rankingDe(id));
        model.addAttribute("resumen", educacionFisicaService.resumenDe(id));
        return "edfisica/ranking";
    }

    @GetMapping("/restricciones")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO') or (hasRole('DOCENTE') and principal.usuario.esDocenteEducacionFisica)")
    public String restricciones(Model model) {
        model.addAttribute("restricciones", educacionFisicaService.todasLasRestricciones());
        model.addAttribute("restriccionForm", new RestriccionMedicaForm());
        model.addAttribute("alumnos", alumnoRepository.findAll());
        return "edfisica/restricciones";
    }

    @PostMapping("/restricciones")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String crearRestriccion(@Valid @ModelAttribute RestriccionMedicaForm form, BindingResult bindingResult,
                                    @RequestParam(required = false) MultipartFile documento,
                                    @AuthenticationPrincipal SigaiUserPrincipal principal, Model model,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("restricciones", educacionFisicaService.todasLasRestricciones());
            model.addAttribute("alumnos", alumnoRepository.findAll());
            return "edfisica/restricciones";
        }
        educacionFisicaService.crearRestriccion(form, principal.getUsuario(), documento);
        redirectAttributes.addFlashAttribute("mensajeExito", "Restriccion medica registrada.");
        return "redirect:/educacion-fisica/restricciones";
    }
}
