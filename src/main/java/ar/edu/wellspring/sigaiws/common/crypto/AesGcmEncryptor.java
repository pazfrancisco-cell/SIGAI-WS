package ar.edu.wellspring.sigaiws.common.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cifrado simetrico AES-256-GCM para columnas sensibles en reposo
 * (DNI, fecha de nacimiento, domicilio, datos medicos - seccion 7.2).
 * Decision documentada en docs/DECISIONS.md (ADR-004): cifrado a nivel de
 * aplicacion en lugar de Always Encrypted / TDE nativo de SQL Server, para
 * no acoplar el proyecto a una edicion especifica del motor de base de datos.
 *
 * La clave se provee por variable de entorno SIGAI_AES_KEY (base64, 32 bytes).
 * Nunca debe commitearse ni loguearse.
 */
@Component
public class AesGcmEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmEncryptor(@Value("${sigai.crypto.aes-key:}") String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            // Se permite arrancar sin clave solo para entornos de test/dev locales sin
            // datos reales; en produccion la ausencia de clave debe bloquear el arranque
            // (ver EncryptionConfig, que valida el perfil activo).
            this.secretKey = null;
        } else {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        }
    }

    public boolean isConfigured() {
        return secretKey != null;
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        requireKey();
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] result = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(cipherText, 0, result, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cifrar el valor", e);
        }
    }

    public String decrypt(String encoded) {
        if (encoded == null) {
            return null;
        }
        requireKey();
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH_BYTES);
            byte[] cipherText = new byte[decoded.length - IV_LENGTH_BYTES];
            System.arraycopy(decoded, IV_LENGTH_BYTES, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo descifrar el valor", e);
        }
    }

    private void requireKey() {
        if (secretKey == null) {
            throw new IllegalStateException(
                    "SIGAI_AES_KEY no esta configurada. Generar con 'openssl rand -base64 32' y definirla como variable de entorno.");
        }
    }
}
