package ar.edu.wellspring.sigaiws.modules.calificacion.domain;

import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CursoDivision;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.Materia;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Materia que el alumno termino el año con promedio anual desaprobado. Se rinde
 * en las mesas de diciembre y febrero; si pasa de año debiendola, es una previa
 * que puede rendir en las mesas siguientes hasta aprobarla.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "materia_adeudada")
public class MateriaAdeudada {

    public enum Estado { PENDIENTE, APROBADA, RECURSADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    /** Ciclo lectivo en que la curso y la desaprobo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciclo_lectivo_id", nullable = false)
    private CicloLectivo cicloLectivo;

    /** Curso en que la curso: la misma materia se dicta de 1° a 6°, el año dice cual se debe. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_division_id", nullable = false)
    private CursoDivision cursoDivision;

    @Column(name = "promedio_anual")
    private BigDecimal promedioAnual;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado = Estado.PENDIENTE;

    @Column(name = "nota_aprobacion")
    private BigDecimal notaAprobacion;

    @Column(name = "aprobada_en")
    private LocalDate aprobadaEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "turno_aprobacion", length = 20)
    private TurnoMesa turnoAprobacion;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @OneToMany(mappedBy = "materiaAdeudada", fetch = FetchType.LAZY)
    @OrderBy("fecha ASC")
    private List<NotaMesa> notasMesa = new ArrayList<>();

    /** "Matematica (2°, 2026)". */
    public String getDescripcion() {
        return materia.getNombre() + " (" + cursoDivision.getAnio() + "°, " + cicloLectivo.getAnio() + ")";
    }

    public boolean isPendiente() {
        return estado == Estado.PENDIENTE;
    }
}
