package ar.edu.wellspring.sigaiws.common.storage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

/**
 * Seccion 7.2: "acceso exclusivo mediante URL firmada temporal" para documentos
 * sensibles (certificados medicos, etc). Firma HMAC-SHA256 del path + expiracion;
 * ArchivoController rechaza el request si la firma no coincide o si ya expiro.
 * Reutiliza la misma clave que AesGcmEncryptor (sigai.crypto.aes-key) para no
 * sumar un secreto mas a administrar; en dev sin clave configurada usa un
 * secreto fijo NO apto para produccion (misma politica que AesGcmEncryptor).
 */
@Component
public class FirmaArchivoService {

    private static final String ALGORITMO = "HmacSHA256";

    private final byte[] claveSecreta;
    private final long minutosValidez;

    public FirmaArchivoService(@Value("${sigai.crypto.aes-key:}") String base64Key,
                                @Value("${sigai.archivos.url-firma-minutos:15}") long minutosValidez) {
        this.claveSecreta = (base64Key == null || base64Key.isBlank())
                ? "sigai-ws-dev-only-clave-no-apta-produccion".getBytes(StandardCharsets.UTF_8)
                : Base64.getDecoder().decode(base64Key);
        this.minutosValidez = minutosValidez;
    }

    /** Devuelve el query string completo ("path=...&exp=...&sig=...") listo para usar en un href. */
    public String queryFirmado(String path) {
        long exp = Instant.now().getEpochSecond() + (minutosValidez * 60);
        String firma = firmar(path, exp);
        return "path=" + UriUtils.encode(path, StandardCharsets.UTF_8) + "&exp=" + exp + "&sig=" + firma;
    }

    public boolean esValida(String path, long exp, String firmaRecibida) {
        if (Instant.now().getEpochSecond() > exp || firmaRecibida == null) {
            return false;
        }
        String esperada = firmar(path, exp);
        return MessageDigest.isEqual(esperada.getBytes(StandardCharsets.UTF_8), firmaRecibida.getBytes(StandardCharsets.UTF_8));
    }

    private String firmar(String path, long exp) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(claveSecreta, ALGORITMO));
            byte[] resultado = mac.doFinal((path + ":" + exp).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(resultado);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar la URL del archivo", e);
        }
    }
}
