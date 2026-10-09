package ar.edu.wellspring.sigaiws.modules.alumno.web;

import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.common.pdf.PdfTableGenerator;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.EstadoAlumno;
import ar.edu.wellspring.sigaiws.modules.alumno.dto.AlumnoForm;
import ar.edu.wellspring.sigaiws.modules.alumno.dto.PaseAlumnoForm;
import ar.edu.wellspring.sigaiws.modules.alumno.dto.TutorForm;
import ar.edu.wellspring.sigaiws.modules.alumno.service.AlumnoService;
import ar.edu.wellspring.sigaiws.modules.alumno.service.PaseAlumnoService;
import ar.edu.wellspring.sigaiws.modules.alumno.service.TutorService;
import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.deshacer.service.DeshacerService;
import ar.edu.wellspring.sigaiws.modules.disciplina.service.DisciplinaService;
import ar.edu.wellspring.sigaiws.modules.edfisica.service.AptoFisicoService;
import ar.edu.wellspring.sigaiws.modules.edfisica.service.EducacionFisicaService;
import ar.edu.wellspring.sigaiws.modules.horario.repository.AsignacionDocenteRepository;
import ar.edu.wellspring.sigaiws.modules.reporte.service.ReporteService;
import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Modulo 5.1. Permisos segun tabla 5.1.2: lectura para Superadmin/Directivo/
 * Administrativo/Preceptor (todos los alumnos) y Docente (filtrado a los
 * cursos/divisiones donde tiene alguna materia asignada, vista "Limitada" del
 * legajo); escritura (alta, edicion, baja/pase, repeticion, documentos) solo
 * Superadmin/Directivo/Administrativo.
 */
@Controller
@RequestMapping("/alumnos")
@PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','PRECEPTOR','DOCENTE')")
@RequiredArgsConstructor
public class AlumnoController {

    private final AlumnoService alumnoService;
    private final PaseAlumnoService paseAlumnoService;
    private final TutorService tutorService;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final CursoDivisionRepository cursoDivisionRepository;
    private final DisciplinaService disciplinaService;
    private final ReporteService reporteService;
    private final PdfTableGenerator pdfTableGenerator;
    private final EducacionFisicaService educacionFisicaService;
    private final AptoFisicoService aptoFisicoService;
    private final DeshacerService deshacerService;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final ar.edu.wellspring.sigaiws.modules.calificacion.service.MesaExamenService mesaExamenService;

    @GetMapping
    public String listar(@RequestParam(required = false) String texto,
                          @RequestParam(required = false) EstadoAlumno estado,
                          @RequestParam(required = false) Long cursoDivisionId,
                          @RequestParam(required = false) Boolean condicionLibre,
                          @RequestParam(defaultValue = "0") int pagina,
                          @AuthenticationPrincipal SigaiUserPrincipal principal,
                          Model model) {
        Page<Alumno> resultado;
        List<ar.edu.wellspring.sigaiws.modules.configuracion.domain.CursoDivision> cursosDropdown;
        if (principal.getUsuario().tieneRol(RolNombre.DOCENTE)) {
            List<Long> cursosPermitidos = cursosDelDocente(principal.getId());
            if (cursoDivisionId != null && !cursosPermitidos.contains(cursoDivisionId)) {
                cursoDivisionId = null; // filtro de UI fuera de alcance: se ignora, no es un intento de bypass de escritura
            }
            resultado = alumnoService.buscarParaDocente(texto, estado, cursoDivisionId, condicionLibre, cursosPermitidos, PageRequest.of(pagina, 25));
            cursosDropdown = cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc().stream()
                    .filter(cd -> cursosPermitidos.contains(cd.getId())).toList();
        } else {
            resultado = alumnoService.buscar(texto, estado, cursoDivisionId, condicionLibre, PageRequest.of(pagina, 25));
            cursosDropdown = cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc();
        }
        model.addAttribute("alumnos", resultado);
        model.addAttribute("texto", texto);
        model.addAttribute("estado", estado);
        model.addAttribute("cursoDivisionId", cursoDivisionId);
        model.addAttribute("condicionLibre", condicionLibre);
        model.addAttribute("cursosDivisiones", cursosDropdown);
        return "alumnos/list";
    }

    /** Seccion 5.1.5: autocompletado en vivo del tutor al cargar su DNI en el formulario de alumno. */
    @GetMapping("/tutores/buscar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    @ResponseBody
    public Map<String, Object> buscarTutorPorDni(@RequestParam String dni) {
        return tutorService.buscarPorDni(dni)
                .<Map<String, Object>>map(t -> Map.of(
                        "encontrado", true,
                        "apellido", t.getApellido(),
                        "nombre", t.getNombre(),
                        "telefono", t.getTelefono(),
                        "email", t.getEmail()))
                .orElse(Map.of("encontrado", false));
    }

    private List<Long> cursosDelDocente(Long docenteId) {
        return cicloLectivoRepository.findByActivoTrue()
                .map(ciclo -> asignacionDocenteRepository.findAllByDocente_IdAndCicloLectivo_Id(docenteId, ciclo.getId())
                        .stream().map(a -> a.getCursoDivision().getId()).distinct().toList())
                .orElse(List.of());
    }

    /** Seccion 5.1.3: "Accion masiva disponible: exportar seleccionados a PDF/Excel." */
    @PostMapping("/exportar/pdf")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public ResponseEntity<byte[]> exportarPdf(@RequestParam(required = false) List<Long> alumnoIds) {
        List<String> columnas = List.of("Apellido y Nombre", "DNI", "Año/Division", "Estado");
        List<List<String>> filas = new ArrayList<>();
        for (Long id : alumnoIds != null ? alumnoIds : List.<Long>of()) {
            Alumno alumno = alumnoService.obtener(id);
            filas.add(List.of(alumno.getNombreCompleto(), alumno.getDni(), anioDivisionDe(id), alumno.getEstadoGlobal().name()));
        }
        byte[] pdf = pdfTableGenerator.generar("Listado de Alumnos", List.of(), columnas, filas);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"alumnos.pdf\"")
                .body(pdf);
    }

    @PostMapping("/exportar/csv")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public ResponseEntity<byte[]> exportarCsv(@RequestParam(required = false) List<Long> alumnoIds) {
        var buffer = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(buffer, true, StandardCharsets.UTF_8)) {
            writer.write('﻿');
            writer.println("Apellido y Nombre,DNI,Anio/Division,Estado");
            for (Long id : alumnoIds != null ? alumnoIds : List.<Long>of()) {
                Alumno alumno = alumnoService.obtener(id);
                writer.println(String.join(",", csv(alumno.getNombreCompleto()), csv(alumno.getDni()),
                        csv(anioDivisionDe(id)), csv(alumno.getEstadoGlobal().name())));
            }
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"alumnos.csv\"")
                .body(buffer.toByteArray());
    }

    private String anioDivisionDe(Long alumnoId) {
        try {
            var acl = alumnoService.cicloActualDe(alumnoId);
            return acl.getCursoDivision().getNombreCompleto();
        } catch (RuntimeException e) {
            return "-";
        }
    }

    private String csv(String valor) {
        if (valor == null) return "";
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }

    @GetMapping("/{id}")
    public String legajo(@PathVariable Long id, @AuthenticationPrincipal SigaiUserPrincipal principal, Model model) {
        boolean esSoloDocente = principal.getUsuario().tieneRol(RolNombre.DOCENTE);
        if (esSoloDocente && !alumnoService.estaEnCursos(id, cursosDelDocente(principal.getId()))) {
            throw new AccessDeniedException("No tenes acceso al legajo de ese alumno.");
        }
        model.addAttribute("vistaLimitada", esSoloDocente);
        Alumno alumno = alumnoService.obtener(id);
        model.addAttribute("alumno", alumno);
        model.addAttribute("tutores", alumnoService.tutoresDe(id));
        model.addAttribute("trayectoria", alumnoService.trayectoria(id));
        model.addAttribute("materiasAdeudadas", mesaExamenService.deAlumno(id));
        model.addAttribute("documentos", alumnoService.documentosDe(id));
        model.addAttribute("observaciones", disciplinaService.historialDe(id));
        model.addAttribute("firmas", disciplinaService.firmasDe(id));
        model.addAttribute("reportes", reporteService.reportesDe(id));
        model.addAttribute("evolucionEdFisica", educacionFisicaService.evolucionGraficosDe(id));
        model.addAttribute("aptoFisico", aptoFisicoService.obtenerDelCicloActivo(id).orElse(null));
        model.addAttribute("esSaf", aptoFisicoService.esSaf(id));
        try {
            model.addAttribute("cicloActual", alumnoService.cicloActualDe(id));
        } catch (RuntimeException ignored) {
            // Puede no existir trayectoria en el ciclo activo (ej. alumno dado de baja hace años).
        }
        model.addAttribute("cuatrimestreEnCurso", cicloLectivoRepository.findByActivoTrue()
                .map(ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo::getCuatrimestreEnCurso).orElse(1));
        return "alumnos/legajo";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String nuevoForm(Model model) {
        model.addAttribute("alumnoForm", new AlumnoForm());
        cargarDatosFormulario(model);
        return "alumnos/form";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String crear(@Valid @ModelAttribute AlumnoForm form, BindingResult bindingResult,
                         Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            cargarDatosFormulario(model);
            return "alumnos/form";
        }
        alumnoService.crear(form);
        redirectAttributes.addFlashAttribute("mensajeExito", "Alumno registrado correctamente.");
        return "redirect:/alumnos";
    }

    @GetMapping("/{id}/editar")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String editarForm(@PathVariable Long id, Model model) {
        Alumno alumno = alumnoService.obtener(id);
        AlumnoForm form = new AlumnoForm();
        form.setId(alumno.getId());
        form.setApellido(alumno.getApellido());
        form.setNombre(alumno.getNombre());
        form.setDni(alumno.getDni());
        form.setFechaNacimiento(alumno.getFechaNacimiento());
        form.setGenero(alumno.getGenero());
        form.setNacionalidad(alumno.getNacionalidad());
        form.setDomicilio(alumno.getDomicilio());
        form.setLocalidad(alumno.getLocalidad());
        form.setEstado(alumno.getEstadoGlobal());
        form.setCicloLectivoIngresoId(alumno.getCicloLectivoIngreso().getId());
        try {
            var acl = alumnoService.cicloActualDe(id);
            form.setAnio(acl.getCursoDivision().getAnio());
            form.setDivision(acl.getCursoDivision().getDivision());
            form.setRepetidor(acl.isRepetidor());
        } catch (RuntimeException ignored) {
            // sin trayectoria en el ciclo activo
        }
        form.setTutores(alumnoService.tutoresDe(id).stream().map(vinculo -> {
            TutorForm tf = new TutorForm();
            tf.setApellido(vinculo.getTutor().getApellido());
            tf.setNombre(vinculo.getTutor().getNombre());
            tf.setVinculo(vinculo.getVinculo());
            tf.setDni(vinculo.getTutor().getDni());
            tf.setTelefono(vinculo.getTutor().getTelefono());
            tf.setEmail(vinculo.getTutor().getEmail());
            tf.setRecibeNotificaciones(vinculo.isRecibeNotificaciones());
            return tf;
        }).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)));
        model.addAttribute("alumnoForm", form);
        cargarDatosFormulario(model);
        return "alumnos/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String editar(@PathVariable Long id, @Valid @ModelAttribute AlumnoForm form, BindingResult bindingResult,
                          Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            cargarDatosFormulario(model);
            return "alumnos/form";
        }
        alumnoService.editar(id, form);
        redirectAttributes.addFlashAttribute("mensajeExito", "Alumno actualizado correctamente.");
        return "redirect:/alumnos";
    }

    @PostMapping("/{id}/baja")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String darDeBaja(@PathVariable Long id, @RequestParam String motivo, RedirectAttributes redirectAttributes,
                             @AuthenticationPrincipal SigaiUserPrincipal principal) {
        alumnoService.darDeBaja(id, motivo);
        String undoId = deshacerService.registrar(DeshacerService.Tipo.BAJA_O_PASE_ALUMNO, id, principal.getId());
        redirectAttributes.addFlashAttribute("mensajeExito", "Alumno dado de baja.");
        redirectAttributes.addFlashAttribute("undoId", undoId);
        return "redirect:/alumnos/" + id;
    }

    @PostMapping("/{id}/pase")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String registrarPase(@PathVariable Long id, @Valid @ModelAttribute PaseAlumnoForm paseAlumnoForm,
                                 BindingResult bindingResult, RedirectAttributes redirectAttributes,
                                 @AuthenticationPrincipal SigaiUserPrincipal principal) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMensaje", "Complete correctamente los datos del pase.");
            return "redirect:/alumnos/" + id;
        }
        paseAlumnoService.registrarPase(id, paseAlumnoForm);
        String undoId = deshacerService.registrar(DeshacerService.Tipo.BAJA_O_PASE_ALUMNO, id, principal.getId());
        redirectAttributes.addFlashAttribute("mensajeExito", "Pase registrado y documento generado.");
        redirectAttributes.addFlashAttribute("undoId", undoId);
        return "redirect:/alumnos/" + id;
    }

    @PostMapping("/{id}/repeticion")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String registrarRepeticion(@PathVariable Long id, @RequestParam Long cursoDivisionId,
                                       RedirectAttributes redirectAttributes) {
        alumnoService.registrarRepeticion(id, cursoDivisionId);
        redirectAttributes.addFlashAttribute("mensajeExito", "Repeticion de año registrada.");
        return "redirect:/alumnos/" + id;
    }

    @PostMapping("/{id}/foto")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String subirFoto(@PathVariable Long id, @RequestParam MultipartFile foto, RedirectAttributes redirectAttributes) {
        alumnoService.subirFoto(id, foto);
        redirectAttributes.addFlashAttribute("mensajeExito", "Foto actualizada.");
        return "redirect:/alumnos/" + id;
    }

    @PostMapping("/{id}/documentos")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
    public String subirDocumento(@PathVariable Long id, @RequestParam MultipartFile archivo,
                                  @RequestParam String tipo, RedirectAttributes redirectAttributes) {
        alumnoService.subirDocumento(id, archivo, tipo);
        redirectAttributes.addFlashAttribute("mensajeExito", "Documento cargado.");
        return "redirect:/alumnos/" + id;
    }

    /** Seccion 5.7 (extension): registra la presentacion del apto fisico del ciclo lectivo activo. */
    @PostMapping("/{id}/apto-fisico")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO') or (hasRole('DOCENTE') and principal.usuario.esDocenteEducacionFisica)")
    public String presentarAptoFisico(@PathVariable Long id, @RequestParam(required = false) MultipartFile documento,
                                       @AuthenticationPrincipal SigaiUserPrincipal principal, RedirectAttributes redirectAttributes) {
        aptoFisicoService.presentar(id, documento, principal.getUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Apto fisico registrado.");
        return "redirect:/alumnos/" + id;
    }

    private void cargarDatosFormulario(Model model) {
        var cursos = cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc();
        model.addAttribute("cicloLectivos", cicloLectivoRepository.findAll());
        model.addAttribute("cursosDivisiones", cursos);
        model.addAttribute("divisionesDisponibles", cursos.stream().map(c -> c.getDivision()).distinct().sorted().toList());
    }
}
