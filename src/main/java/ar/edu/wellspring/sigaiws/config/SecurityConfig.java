package ar.edu.wellspring.sigaiws.config;

import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

/**
 * Seccion 3.4 / 7.1: autenticacion por sesion de servidor con cookies
 * HttpOnly/Secure/SameSite=Strict (configuradas en application.yml,
 * server.servlet.session.cookie.*). Decision documentada en docs/DECISIONS.md
 * ADR-001 (sesiones en vez de JWT, coherente con la arquitectura MVC monolitica
 * de la seccion 3.3).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
            "/login", "/olvide-password", "/restablecer-password",
            "/css/**", "/js/**", "/img/**", "/fonts/**", "/webjars/**", "/error/**"
    };

    @Bean
    public DefaultSecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(RUTAS_PUBLICAS).permitAll()
                    .anyRequest().authenticated()
            )
            .formLogin(form -> form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .usernameParameter("email")
                    .passwordParameter("password")
                    .successHandler(successHandler())
                    .failureHandler(failureHandler())
                    .permitAll()
            )
            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
            );
            // CSRF queda habilitado (comportamiento por defecto de Spring Security):
            // obligatorio para una app MVC con formularios y cookies de sesion (OWASP).
            // Los formularios Thymeleaf incluyen el token via thymeleaf-extras-springsecurity6.

        return http.build();
    }

    /**
     * Redirige segun si el usuario debe cambiar su contraseña (alta reciente o
     * reseteo administrativo, seccion 7.1: "Las contraseñas temporales... obligan
     * al cambio en el primer inicio de sesion").
     */
    private AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            Authentication auth = authentication;
            Object principal = auth.getPrincipal();
            String destino = "/";
            if (principal instanceof SigaiUserPrincipal sigaiUser && sigaiUser.debeCambiarPassword()) {
                destino = "/cambiar-password";
            }
            response.sendRedirect(request.getContextPath() + destino);
        };
    }

    private AuthenticationFailureHandler failureHandler() {
        return new AuthenticationFailureHandler() {
            private final SimpleUrlAuthenticationFailureHandler delegate = new SimpleUrlAuthenticationFailureHandler();

            @Override
            public void onAuthenticationFailure(jakarta.servlet.http.HttpServletRequest request,
                                                  jakarta.servlet.http.HttpServletResponse response,
                                                  AuthenticationException exception) throws IOException, jakarta.servlet.ServletException {
                String motivo = exception instanceof LockedException ? "bloqueado" : "credenciales";
                delegate.setDefaultFailureUrl("/login?error=" + motivo);
                delegate.onAuthenticationFailure(request, response, exception);
            }
        };
    }
}
