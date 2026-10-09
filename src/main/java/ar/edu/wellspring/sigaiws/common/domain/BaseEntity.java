package ar.edu.wellspring.sigaiws.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Superclase para todas las entidades persistentes. Provee identidad tecnica
 * (id numerico, no expuesto en URLs publicas) y metadatos de auditoria basica
 * (quien/cuando crea o modifica un registro), independiente de la tabla
 * {@code audit_log} detallada (seccion 3.5 del documento), que registra el
 * detalle de cada accion con valores anteriores/nuevos.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @LastModifiedDate
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @CreatedBy
    @Column(name = "creado_por", updatable = false, length = 120)
    private String creadoPor;

    @LastModifiedBy
    @Column(name = "actualizado_por", length = 120)
    private String actualizadoPor;
}
