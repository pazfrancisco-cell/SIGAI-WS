package ar.edu.wellspring.sigaiws.modules.alumno.domain;

import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CursoDivision;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.Workshop;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Un registro por alumno y ciclo lectivo: ES la "trayectoria" (seccion 5.1.4
 * tab Trayectoria) y a la vez el mecanismo de reinicio anual de contadores
 * de asistencia/disciplina (seccion 5.14.2, 5.4.3). Se crea al matricular al
 * alumno y, para años siguientes, cuando se activa un nuevo ciclo lectivo
 * (ver CicloLectivoActivadoEvent / AlumnoCicloLectivoEventListener).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "alumno_ciclo_lectivo")
public class AlumnoCicloLectivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciclo_lectivo_id", nullable = false)
    private CicloLectivo cicloLectivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_division_id", nullable = false)
    private CursoDivision cursoDivision;

    @Column(name = "repetidor", nullable = false)
    private boolean repetidor = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_c1_id")
    private Workshop workshopC1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_c2_id")
    private Workshop workshopC2;

    @Column(name = "inasistencias_acumuladas", nullable = false)
    private BigDecimal inasistenciasAcumuladas = BigDecimal.ZERO;

    @Column(name = "observaciones_acumuladas", nullable = false)
    private Integer observacionesAcumuladas = 0;

    @Column(name = "firmas_acumuladas", nullable = false)
    private Integer firmasAcumuladas = 0;

    @Column(name = "condicion_libre", nullable = false)
    private boolean condicionLibre = false;

    /** Seccion 5.2.3 / ADR-008: null hasta cruzar 20 faltas; despues, 21 o 28 segun el % justificado en ese momento. */
    @Column(name = "umbral_condicion_libre_efectivo")
    private Integer umbralCondicionLibreEfectivo;

    @Column(name = "condicion_libre_desde")
    private LocalDateTime condicionLibreDesde;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();
}
