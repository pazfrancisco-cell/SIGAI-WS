package ar.edu.wellspring.sigaiws.modules.alumno.importacion;

import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Seccion 5.14.2: importacion de alumnos y tutores desde Excel/CSV, con vista previa y correccion de errores en pantalla. */
@Controller
@RequestMapping("/alumnos/importar")
@PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
@RequiredArgsConstructor
public class ImportacionAlumnosController {

    private static final String REDIRECT = "redirect:/alumnos/importar";
    private static final int MAX_FILAS_EDITABLES = 150;

    private final LectorArchivoImportacion lector;
    private final ImportacionAlumnosService importacionService;
    private final CicloLectivoRepository cicloLectivoRepository;

    @GetMapping
    public String pantalla(HttpSession session, Model model) {
        ImportacionAlumnos importacion = enCurso(session);
        model.addAttribute("importacion", importacion);
        if (importacion != null) {
            // Tomcat acepta hasta 10.000 parametros por formulario: se corrigen de a tandas.
            var conErrores = importacion.conErrores();
            model.addAttribute("filasEditables", conErrores.subList(0, Math.min(conErrores.size(), MAX_FILAS_EDITABLES)));
            model.addAttribute("filasEditablesRestantes", Math.max(0, conErrores.size() - MAX_FILAS_EDITABLES));
        }
        model.addAttribute("columnas", ImportacionAlumnos.COLUMNAS);
        model.addAttribute("cicloActivo", cicloLectivoRepository.findByActivoTrue().orElse(null));
        return "alumnos/importar";
    }

    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> plantilla() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla-importacion-alumnos.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(importacionService.plantilla());
    }

    @PostMapping
    public String subir(@RequestParam("archivo") MultipartFile archivo, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            ImportacionAlumnos importacion = lector.leer(archivo);
            importacionService.validar(importacion);
            session.setAttribute(ImportacionAlumnos.ATRIBUTO_SESION, importacion);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMensaje", e.getMessage());
        }
        return REDIRECT;
    }

    @PostMapping("/revalidar")
    public String revalidar(@RequestParam Map<String, String> parametros, HttpSession session, RedirectAttributes redirectAttributes) {
        ImportacionAlumnos importacion = enCurso(session);
        if (importacion == null) {
            return REDIRECT;
        }
        importacionService.aplicarCorrecciones(importacion, parametros);
        importacionService.validar(importacion);
        session.setAttribute(ImportacionAlumnos.ATRIBUTO_SESION, importacion);
        int conErrores = importacion.conErrores().size();
        redirectAttributes.addFlashAttribute(conErrores == 0 ? "mensajeExito" : "errorMensaje", conErrores == 0
                ? "Todas las filas pendientes estan correctas. Ya podes importarlas."
                : "Quedan " + conErrores + " filas con errores.");
        return REDIRECT;
    }

    @PostMapping("/confirmar")
    public String confirmar(@RequestParam Map<String, String> parametros, HttpSession session, RedirectAttributes redirectAttributes) {
        ImportacionAlumnos importacion = enCurso(session);
        if (importacion == null) {
            return REDIRECT;
        }
        importacionService.aplicarCorrecciones(importacion, parametros);
        importacionService.validar(importacion);
        try {
            var r = importacionService.importarValidas(importacion);
            String mensaje = "Se importaron " + r.importados() + " alumnos"
                    + (r.cuentasPortalCreadas() > 0 ? " y se crearon " + r.cuentasPortalCreadas() + " cuentas del Portal de Padres" : "") + ".";
            int pendientes = importacion.conErrores().size();
            if (pendientes > 0) {
                mensaje += " Quedan " + pendientes + " filas con errores para corregir.";
            }
            redirectAttributes.addFlashAttribute(r.importados() > 0 ? "mensajeExito" : "errorMensaje", mensaje);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMensaje", e.getMessage());
        }
        session.setAttribute(ImportacionAlumnos.ATRIBUTO_SESION, importacion);
        return REDIRECT;
    }

    /** Saca una fila de la importacion (por ejemplo, un alumno que no corresponde). */
    @PostMapping("/fila/{numero}/quitar")
    public String quitarFila(@PathVariable int numero, HttpSession session) {
        ImportacionAlumnos importacion = enCurso(session);
        if (importacion != null) {
            importacion.getFilas().removeIf(f -> f.getNumero() == numero && !f.isImportada());
            session.setAttribute(ImportacionAlumnos.ATRIBUTO_SESION, importacion);
        }
        return REDIRECT;
    }

    @PostMapping("/descartar")
    public String descartar(HttpSession session) {
        session.removeAttribute(ImportacionAlumnos.ATRIBUTO_SESION);
        return REDIRECT;
    }

    private static ImportacionAlumnos enCurso(HttpSession session) {
        return (ImportacionAlumnos) session.getAttribute(ImportacionAlumnos.ATRIBUTO_SESION);
    }
}
