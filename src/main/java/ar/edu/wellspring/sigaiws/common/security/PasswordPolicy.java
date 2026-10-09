package ar.edu.wellspring.sigaiws.common.security;

import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Politica de complejidad de contraseñas (seccion 7.1): "Minimo de complejidad
 * de contraseña: 8 caracteres, al menos 1 mayuscula, 1 numero, 1 caracter especial."
 * Usada por los flujos de cambio de contraseña y recuperacion (modulo auth).
 *
 * Constructor injection (en vez de @Value en el campo) para que sea testeable
 * sin necesidad de levantar contexto de Spring.
 */
@Component
public class PasswordPolicy {

    private static final Pattern MAYUSCULA = Pattern.compile("[A-Z]");
    private static final Pattern NUMERO = Pattern.compile("[0-9]");
    private static final Pattern ESPECIAL = Pattern.compile("[^A-Za-z0-9]");

    private final int longitudMinima;

    public PasswordPolicy(@Value("${sigai.seguridad.password-min-length:8}") int longitudMinima) {
        this.longitudMinima = longitudMinima;
    }

    public void validar(String password) {
        if (password == null || password.length() < longitudMinima) {
            throw new BusinessException("La contraseña debe tener al menos " + longitudMinima + " caracteres.");
        }
        if (!MAYUSCULA.matcher(password).find()) {
            throw new BusinessException("La contraseña debe incluir al menos una letra mayuscula.");
        }
        if (!NUMERO.matcher(password).find()) {
            throw new BusinessException("La contraseña debe incluir al menos un numero.");
        }
        if (!ESPECIAL.matcher(password).find()) {
            throw new BusinessException("La contraseña debe incluir al menos un caracter especial.");
        }
    }
}
