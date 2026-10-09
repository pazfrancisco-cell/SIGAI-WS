package ar.edu.wellspring.sigaiws.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tabla de auditoria institucional (seccion 3.5): "de solo lectura para todos
 * los roles excepto el Superadministrador, y nunca puede ser eliminada desde
 * la interfaz del sistema". Por eso esta entidad es deliberadamente inmutable
 * (sin setters, sin metodo de borrado expuesto por AuditLogRepository) y no
 * extiende BaseEntity: un registro de auditoria no se "actualiza".
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_email", length = 180)
    private String usuarioEmail;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "modulo", nullable = false, length = 60)
    private String modulo;

    @Column(name = "accion", nullable = false, length = 60)
    private String accion;

    @Column(name = "entidad", length = 120)
    private String entidad;

    @Column(name = "registro_id", length = 60)
    private String registroId;

    @Lob
    @Column(name = "valores_anteriores")
    private String valoresAnteriores;

    @Lob
    @Column(name = "valores_nuevos")
    private String valoresNuevos;

    public AuditLog(Long usuarioId, String usuarioEmail, String ipAddress, String modulo, String accion,
                     String entidad, String registroId, String valoresAnteriores, String valoresNuevos) {
        this.usuarioId = usuarioId;
        this.usuarioEmail = usuarioEmail;
        this.timestamp = Instant.now();
        this.ipAddress = ipAddress;
        this.modulo = modulo;
        this.accion = accion;
        this.entidad = entidad;
        this.registroId = registroId;
        this.valoresAnteriores = valoresAnteriores;
        this.valoresNuevos = valoresNuevos;
    }
}
