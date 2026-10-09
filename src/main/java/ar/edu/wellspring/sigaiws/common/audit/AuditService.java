package ar.edu.wellspring.sigaiws.common.audit;

import ar.edu.wellspring.sigaiws.common.security.AuthenticatedActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Punto unico de escritura del audit_log (seccion 3.5). Se invoca explicitamente
 * desde los services de cada modulo en los puntos que la Propuesta Tecnica marca
 * como auditables: alta/baja/edicion de alumnos y usuarios, justificaciones,
 * cambios de configuracion, exportaciones, login/logout, cambios de estado, etc.
 *
 * Se prefirio invocacion explicita (en vez de un aspecto AOP generico que
 * intercepte todos los @Transactional) porque cada operacion tiene una nocion
 * distinta de "antes/despues" (un alta no tiene "antes", una baja no tiene
 * "despues") y la version explicita es mas facil de auditar y mantener.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public void registrar(String modulo, String accion, String entidad, Object registroId,
                           Object valoresAnteriores, Object valoresNuevos) {
        try {
            AuthenticatedActor actor = actorActual();
            AuditLog log = new AuditLog(
                    actor != null ? actor.getId() : null,
                    actor != null ? actor.getUsername() : "sistema",
                    ipActual(),
                    modulo,
                    accion,
                    entidad,
                    registroId != null ? registroId.toString() : null,
                    toJson(valoresAnteriores),
                    toJson(valoresNuevos)
            );
            auditLogRepository.save(log);
        } catch (Exception e) {
            // La auditoria nunca debe hacer fallar la operacion de negocio que la origina.
            log.error("No se pudo registrar auditoria [{}] [{}] sobre {}", modulo, accion, entidad, e);
        }
    }

    /** Variante para acciones sin usuario autenticado en contexto (ej. intento de login fallido). */
    public void registrarAnonimo(String modulo, String accion, String detalle) {
        AuditLog auditLog = new AuditLog(null, detalle, ipActual(), modulo, accion, null, null, null, null);
        auditLogRepository.save(auditLog);
    }

    private AuthenticatedActor actorActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof AuthenticatedActor actor) {
            return actor;
        }
        return null;
    }

    private String ipActual() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
