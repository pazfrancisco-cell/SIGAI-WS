package ar.edu.wellspring.sigaiws.modules.calificacion.domain;

import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Seccion 5.3.6: flujo de correccion sobre periodo cerrado. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "solicitud_correccion_nota")
public class SolicitudCorreccionNota {

    public enum Estado { PENDIENTE, APROBADA, RECHAZADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nota_id", nullable = false)
    private Nota nota;

    @Column(name = "nota_anterior", nullable = false)
    private BigDecimal notaAnterior;

    @Column(name = "nota_propuesta", nullable = false)
    private BigDecimal notaPropuesta;

    @Column(name = "justificacion", nullable = false, length = 500)
    private String justificacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado = Estado.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitado_por_id", nullable = false)
    private Usuario solicitadoPor;

    @Column(name = "solicitado_en", nullable = false)
    private LocalDateTime solicitadoEn = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resuelto_por_id")
    private Usuario resueltoPor;

    @Column(name = "resuelto_en")
    private LocalDateTime resueltoEn;

    @Column(name = "motivo_rechazo", length = 300)
    private String motivoRechazo;
}
