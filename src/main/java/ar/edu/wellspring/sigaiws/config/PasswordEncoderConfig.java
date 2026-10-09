package ar.edu.wellspring.sigaiws.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seccion 7.1: "Contraseñas almacenadas con algoritmo bcrypt (factor de coste minimo 12)".
 * El factor de coste es configurable por sigai.seguridad.bcrypt-strength para poder
 * ajustarlo por entorno sin recompilar.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder(@Value("${sigai.seguridad.bcrypt-strength:12}") int strength) {
        return new BCryptPasswordEncoder(strength);
    }
}
