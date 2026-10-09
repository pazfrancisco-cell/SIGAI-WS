package ar.edu.wellspring.sigaiws.modules.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Seccion 7.1: mientras Usuario.debeCambiarPassword sea true, ninguna otra
 * pantalla del sistema debe ser accesible; se fuerza el redirect a
 * /cambiar-password en cada request autenticado.
 */
@Component
public class ForcePasswordChangeInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Object principalObj = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;

        if (principalObj instanceof SigaiUserPrincipal principal && principal.debeCambiarPassword()) {
            String uri = request.getRequestURI();
            if (!uri.endsWith("/cambiar-password") && !uri.endsWith("/logout")) {
                response.sendRedirect(request.getContextPath() + "/cambiar-password");
                return false;
            }
        }
        return true;
    }
}
