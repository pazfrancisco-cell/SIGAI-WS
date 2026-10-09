package ar.edu.wellspring.sigaiws.modules.workshop.domain;

import ar.edu.wellspring.sigaiws.modules.configuracion.domain.Aula;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Fase 4: horario fijo semanal del workshop. El docente sale de workshop_docente (1 a 1). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workshop_horario_slot")
public class WorkshopHorarioSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workshop_id", nullable = false)
    private Workshop workshop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciclo_lectivo_id", nullable = false)
    private CicloLectivo cicloLectivo;

    /** 1=Lunes .. 5=Viernes. */
    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana;

    @Column(name = "periodo_orden", nullable = false)
    private Integer periodoOrden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aula_id")
    private Aula aula;
}
