package ar.edu.wellspring.sigaiws.modules.alerta.domain;

import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Modulo 5.13 (Alertas y Seguimiento Automatico). Catalogo de tipos en seccion 5.13.2. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "alerta")
public class Alerta {

    public enum Prioridad { INFO, MEDIA, ALTA, CRITICA }
    public enum Estado { NUEVA, EN_SEGUIMIENTO, RESUELTA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo", nullable = false, length = 60)
    private String tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad", nullable = false, length = 10)
    private Prioridad prioridad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id")
    private Alumno alumno;

    @Column(name = "descripcion", nullable = false, length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado = Estado.NUEVA;

    @Column(name = "generada_en", nullable = false)
    private LocalDateTime generadaEn = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resuelta_por_id")
    private Usuario resueltaPor;

    @Column(name = "resuelta_en")
    private LocalDateTime resueltaEn;

    @Column(name = "nota_resolucion", length = 500)
    private String notaResolucion;

    /** Lo que genero la alerta (ej. SOLICITUD_CORRECCION 15, MATERIA 3), para no duplicarla y poder cerrarla sola. */
    @Column(name = "entidad_relacionada", length = 60)
    private String entidadRelacionada;

    @Column(name = "entidad_relacionada_id")
    private Long entidadRelacionadaId;
}
