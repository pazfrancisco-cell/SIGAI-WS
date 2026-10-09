package ar.edu.wellspring.sigaiws.common.audit;

import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Seccion 7.3: "El Superadministrador puede consultar el audit_log con filtros
 * por usuario, fecha, modulo y tipo de accion. Exportacion del log disponible
 * en formato CSV para analisis externo."
 */
@Controller
@RequestMapping("/auditoria")
@PreAuthorize("hasRole('SUPERADMIN')")
public class AuditoriaController {

    private final AuditLogRepository auditLogRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaController(AuditLogRepository auditLogRepository, UsuarioRepository usuarioRepository) {
        this.auditLogRepository = auditLogRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) Long usuarioId,
                          @RequestParam(required = false) String modulo,
                          @RequestParam(required = false) String accion,
                          @RequestParam(required = false) LocalDate desde,
                          @RequestParam(required = false) LocalDate hasta,
                          @RequestParam(defaultValue = "0") int pagina,
                          Model model) {
        Instant desdeInstant = desde != null ? desde.atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        Instant hastaInstant = hasta != null ? hasta.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;

        model.addAttribute("registros", auditLogRepository.buscar(usuarioId, vacioANulo(modulo), vacioANulo(accion),
                desdeInstant, hastaInstant, PageRequest.of(pagina, 30)));
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("modulos", auditLogRepository.modulosDistintos());
        model.addAttribute("acciones", auditLogRepository.accionesDistintas());
        model.addAttribute("usuarioId", usuarioId);
        model.addAttribute("modulo", modulo);
        model.addAttribute("accion", accion);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        return "auditoria/listado";
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportarCsv(@RequestParam(required = false) Long usuarioId,
                                               @RequestParam(required = false) String modulo,
                                               @RequestParam(required = false) String accion,
                                               @RequestParam(required = false) LocalDate desde,
                                               @RequestParam(required = false) LocalDate hasta) {
        Instant desdeInstant = desde != null ? desde.atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        Instant hastaInstant = hasta != null ? hasta.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;

        var registros = auditLogRepository.buscarTodos(usuarioId, vacioANulo(modulo), vacioANulo(accion), desdeInstant, hastaInstant);

        var buffer = new java.io.ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(buffer, true, StandardCharsets.UTF_8)) {
            writer.write('﻿'); // BOM: que Excel reconozca UTF-8 al abrir el CSV
            writer.println("fecha,usuario,ip,modulo,accion,entidad,registro_id,valores_anteriores,valores_nuevos");
            for (AuditLog registro : registros) {
                writer.println(String.join(",",
                        csv(registro.getTimestamp().toString()),
                        csv(registro.getUsuarioEmail()),
                        csv(registro.getIpAddress()),
                        csv(registro.getModulo()),
                        csv(registro.getAccion()),
                        csv(registro.getEntidad()),
                        csv(registro.getRegistroId()),
                        csv(registro.getValoresAnteriores()),
                        csv(registro.getValoresNuevos())));
            }
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_log.csv\"")
                .body(buffer.toByteArray());
    }

    private String vacioANulo(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor;
    }

    private String csv(String valor) {
        if (valor == null) return "";
        return "\"" + valor.replace("\"", "\"\"").replace("\n", " ") + "\"";
    }
}
