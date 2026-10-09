package ar.edu.wellspring.sigaiws.modules.usuario.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Genera contraseñas temporales que cumplen la politica de complejidad de la
 * seccion 7.1 (minimo 8 caracteres, 1 mayuscula, 1 numero, 1 caracter especial).
 * Se entregan al usuario por email y deben cambiarse en el primer ingreso
 * (Usuario.debeCambiarPassword).
 */
@Component
public class PasswordGenerator {

    private static final String MAYUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnpqrstuvwxyz";
    private static final String NUMEROS = "23456789";
    private static final String ESPECIALES = "!@#$%&*";
    private static final String TODOS = MAYUSCULAS + MINUSCULAS + NUMEROS + ESPECIALES;

    private final SecureRandom random = new SecureRandom();

    public String generarTemporal() {
        StringBuilder sb = new StringBuilder(12);
        sb.append(MAYUSCULAS.charAt(random.nextInt(MAYUSCULAS.length())));
        sb.append(NUMEROS.charAt(random.nextInt(NUMEROS.length())));
        sb.append(ESPECIALES.charAt(random.nextInt(ESPECIALES.length())));
        for (int i = 3; i < 12; i++) {
            sb.append(TODOS.charAt(random.nextInt(TODOS.length())));
        }
        // mezcla para que las posiciones fijas de arriba no sean predecibles
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}
