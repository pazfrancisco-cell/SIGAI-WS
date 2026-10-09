package ar.edu.wellspring.sigaiws.web;

import ar.edu.wellspring.sigaiws.common.storage.FirmaArchivoService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sirve archivos guardados por AlmacenamientoService (fotos, documentos de
 * legajo, PDFs de pase). Requiere sesion autenticada (regla global de
 * SecurityConfig: "anyRequest().authenticated()") Y una firma HMAC valida y
 * no vencida generada por FirmaArchivoService (seccion 7.2: "acceso exclusivo
 * mediante URL firmada temporal"). Las vistas generan el link completo con
 * ${@firmaArchivoService.queryFirmado(path)}; un link a /archivos sin firma
 * (o con una vencida/adulterada) responde 403.
 *
 * El path se guarda siempre como "subcarpeta/uuid.ext" (nunca el nombre
 * original) por lo que tampoco es enumerable.
 */
@RestController
@RequestMapping("/archivos")
@RequiredArgsConstructor
public class ArchivoController {

    private final FirmaArchivoService firmaArchivoService;

    @Value("${sigai.archivos.storage-path:./storage}")
    private String storagePath;

    @GetMapping
    public ResponseEntity<Resource> descargar(@RequestParam String path, @RequestParam long exp,
                                               @RequestParam String sig) throws IOException {
        if (!firmaArchivoService.esValida(path, exp, sig)) {
            return ResponseEntity.status(403).build();
        }

        Path base = Path.of(storagePath).normalize().toAbsolutePath();
        Path solicitado = base.resolve(path).normalize().toAbsolutePath();

        if (!solicitado.startsWith(base) || !Files.exists(solicitado)) {
            return ResponseEntity.notFound().build();
        }

        Resource recurso = new UrlResource(solicitado.toUri());
        String contentType = Files.probeContentType(solicitado);
        return ResponseEntity.ok()
                .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + solicitado.getFileName() + "\"")
                .body(recurso);
    }
}
