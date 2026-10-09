package ar.edu.wellspring.sigaiws.modules.reporte.web;

import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.reporte.service.ReporteService;
import ar.edu.wellspring.sigaiws.modules.reporte.service.ReportesAdicionalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Seccion 5.8.3 (Reportes Adicionales). Acceso: Administrativo, Directivo (igual que 5.8.2). */
@Controller
@RequestMapping("/reportes")
@PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO')")
@RequiredArgsConstructor
public class ReportesAdicionalesController {

    private final ReportesAdicionalesService reportesAdicionalesService;
    private final ReporteService reporteService;
    private final CursoDivisionRepository cursoDivisionRepository;

    @GetMapping
    public String panel(Model model) {
        model.addAttribute("cursosDivisiones", cursoDivisionRepository.findAllByOrderByAnioAscDivisionAsc());
        return "reportes/panel";
    }

    /** Seccion 5.8.4: generacion masiva de boletines de un curso/division, entregados en un .zip. */
    @GetMapping("/generar")
    public ResponseEntity<byte[]> generarMasivo(@RequestParam Long cursoDivisionId, @RequestParam int cuatrimestre,
                                                 @AuthenticationPrincipal SigaiUserPrincipal principal) {
        byte[] zip = reporteService.generarBoletinesCursoZip(cursoDivisionId, cuatrimestre, principal.getUsuario());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"boletines_C" + cuatrimestre + ".zip\"")
                .body(zip);
    }

    @GetMapping("/condicion-libre")
    public ResponseEntity<byte[]> condicionLibre() {
        return pdf(reportesAdicionalesService.reporteCondicionLibre(), "alumnos_condicion_libre.pdf");
    }

    @GetMapping("/condicion-libre.xlsx")
    public ResponseEntity<byte[]> condicionLibreExcel() {
        return excel(reportesAdicionalesService.reporteCondicionLibreExcel(), "alumnos_condicion_libre.xlsx");
    }

    @GetMapping("/disciplinario-curso/{cursoDivisionId}")
    public ResponseEntity<byte[]> disciplinarioCurso(@PathVariable Long cursoDivisionId) {
        return pdf(reportesAdicionalesService.reporteDisciplinarioCurso(cursoDivisionId), "reporte_disciplinario_curso.pdf");
    }

    @GetMapping("/disciplinario-curso/{cursoDivisionId}.xlsx")
    public ResponseEntity<byte[]> disciplinarioCursoExcel(@PathVariable Long cursoDivisionId) {
        return excel(reportesAdicionalesService.reporteDisciplinarioCursoExcel(cursoDivisionId), "reporte_disciplinario_curso.xlsx");
    }

    @GetMapping("/rendimiento-materia/{cursoDivisionId}")
    public ResponseEntity<byte[]> rendimientoMateria(@PathVariable Long cursoDivisionId) {
        return pdf(reportesAdicionalesService.reporteRendimientoPorMateria(cursoDivisionId), "reporte_rendimiento_materia.pdf");
    }

    @GetMapping("/rendimiento-materia/{cursoDivisionId}.xlsx")
    public ResponseEntity<byte[]> rendimientoMateriaExcel(@PathVariable Long cursoDivisionId) {
        return excel(reportesAdicionalesService.reporteRendimientoPorMateriaExcel(cursoDivisionId), "reporte_rendimiento_materia.xlsx");
    }

    @GetMapping("/asistencia-curso/{cursoDivisionId}")
    public ResponseEntity<byte[]> asistenciaCurso(@PathVariable Long cursoDivisionId) {
        return pdf(reportesAdicionalesService.reporteAsistenciaCurso(cursoDivisionId), "reporte_asistencia_curso.pdf");
    }

    @GetMapping("/asistencia-curso/{cursoDivisionId}.xlsx")
    public ResponseEntity<byte[]> asistenciaCursoExcel(@PathVariable Long cursoDivisionId) {
        return excel(reportesAdicionalesService.reporteAsistenciaCursoExcel(cursoDivisionId), "reporte_asistencia_curso.xlsx");
    }

    @GetMapping("/asistencia-alumno/{alumnoId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTIVO','ADMINISTRATIVO','PRECEPTOR')")
    public ResponseEntity<byte[]> asistenciaAlumno(@PathVariable Long alumnoId) {
        return pdf(reportesAdicionalesService.reporteAsistenciaAlumno(alumnoId), "reporte_asistencia_alumno.pdf");
    }

    private ResponseEntity<byte[]> pdf(byte[] contenido, String nombreArchivo) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombreArchivo + "\"")
                .body(contenido);
    }

    private ResponseEntity<byte[]> excel(byte[] contenido, String nombreArchivo) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .body(contenido);
    }
}
