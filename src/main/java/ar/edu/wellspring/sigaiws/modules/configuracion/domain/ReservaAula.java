package ar.edu.wellspring.sigaiws.modules.configuracion.domain;

import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reserva puntual de un aula por un docente (seccion 5.5.1): distinta del
 * horario_slot semanal fijo, es para un uso ocasional en una fecha concreta
 * (laboratorio, actividad especial, reunion, etc.) con un curso/division.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reserva_aula")
public class ReservaAula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aula_id", nullable = false)
    private Aula aula;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false)
    private Usuario docente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_division_id", nullable = false)
    private CursoDivision cursoDivision;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "periodo_orden", nullable = false)
    private Integer periodoOrden;

    @Column(name = "motivo", nullable = false, length = 200)
    private String motivo;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
