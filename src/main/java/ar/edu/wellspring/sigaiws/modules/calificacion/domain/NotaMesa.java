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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Resultado de una materia adeudada en una mesa de examen (una por turno y año; se puede corregir). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "nota_mesa")
public class NotaMesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_adeudada_id", nullable = false)
    private MateriaAdeudada materiaAdeudada;

    @Enumerated(EnumType.STRING)
    @Column(name = "turno", nullable = false, length = 20)
    private TurnoMesa turno;

    /** Año calendario de la mesa (la de febrero 2027 cierra el ciclo 2026). */
    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    /** Null si estuvo ausente. */
    @Column(name = "nota")
    private BigDecimal nota;

    @Column(name = "ausente", nullable = false)
    private boolean ausente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cargado_por_id", nullable = false)
    private Usuario cargadoPor;

    @Column(name = "cargado_en", nullable = false)
    private LocalDateTime cargadoEn = LocalDateTime.now();
}
