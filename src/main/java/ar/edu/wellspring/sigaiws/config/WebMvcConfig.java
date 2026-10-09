package ar.edu.wellspring.sigaiws.config;

import ar.edu.wellspring.sigaiws.modules.auth.security.ForcePasswordChangeInterceptor;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

/**
 * Soporte de idioma para el selector ES|EN del topbar (seccion 4.4).
 * NOTA (ver docs/DECISIONS.md): el documento no define contenido en ingles;
 * el mecanismo de i18n queda completo y funcional, pero los archivos
 * messages_en.properties solo cubren las etiquetas de navegacion/UI genericas
 * hasta que la institucion provea las traducciones de los textos de negocio.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ForcePasswordChangeInterceptor forcePasswordChangeInterceptor;

    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(Locale.of("es", "AR"));
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
        registry.addInterceptor(forcePasswordChangeInterceptor)
                .excludePathPatterns("/login", "/logout", "/css/**", "/js/**", "/img/**", "/fonts/**",
                        "/olvide-password", "/restablecer-password");
    }
}
