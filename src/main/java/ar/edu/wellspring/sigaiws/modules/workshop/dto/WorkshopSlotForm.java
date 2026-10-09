package ar.edu.wellspring.sigaiws.modules.workshop.dto;

import lombok.Getter;
import lombok.Setter;

/** Fase 4: asignacion de un bloque fijo semanal (dia+periodo, aula opcional) a un workshop. */
@Getter
@Setter
public class WorkshopSlotForm {
    private Long workshopId;
    private Integer diaSemana;
    private Integer periodoOrden;
    private Long aulaId;
}
