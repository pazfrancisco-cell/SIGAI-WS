package ar.edu.wellspring.sigaiws.modules.configuracion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Estructura del dia escolar (seccion 5.5.2 / 5.14.2), sembrada por V13, editable por Superadmin. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "periodo_horario")
public class PeriodoHorario {

    public enum Tipo { CLASE, RECREO, ALMUERZO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orden", nullable = false, unique = true)
    private Integer orden;

    @Column(name = "nombre", nullable = false, length = 40)
    private String nombre;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private Tipo tipo;
}
