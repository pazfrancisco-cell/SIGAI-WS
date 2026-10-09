package ar.edu.wellspring.sigaiws.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Genera un hash determinista (SHA-256) para campos que estan cifrados en la
 * columna principal (ver EncryptedStringConverter) pero que igualmente deben
 * poder buscarse/validarse por unicidad (ej. DNI de alumnos y tutores).
 * El hash NO permite reconstruir el valor original.
 */
public final class HashUtil {

    private HashUtil() {
    }

    public static String sha256(String value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }
}
