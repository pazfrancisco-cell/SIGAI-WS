package ar.edu.wellspring.sigaiws.config;

import ar.edu.wellspring.sigaiws.common.crypto.AesGcmEncryptor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fail-fast: en cualquier perfil que no sea dev/test, el sistema no debe arrancar
 * sin una clave de cifrado configurada, porque los datos de menores de edad
 * (seccion 7.2) dependen de ella para persistirse correctamente.
 */
@Component
@RequiredArgsConstructor
public class EncryptionConfig {

    private final AesGcmEncryptor encryptor;
    private final Environment environment;

    @Value("${sigai.crypto.aes-key:}")
    private String aesKey;

    @PostConstruct
    public void validar() {
        boolean esDevOTest = environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev", "test"));
        if (!esDevOTest && !encryptor.isConfigured()) {
            throw new IllegalStateException(
                    "SIGAI_AES_KEY es obligatoria fuera de los perfiles dev/test. "
                            + "Generarla con 'openssl rand -base64 32' antes de desplegar.");
        }
    }
}
