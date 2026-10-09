package ar.edu.wellspring.sigaiws.modules.horario.dto;

import lombok.Getter;
import lombok.Setter;

/** Seccion 5.5.3: modal de asignacion de slot (Materia*, Docente*, Aula opcional). */
@Getter
@Setter
public class SlotForm {
    private Long cursoDivisionId;
    private Integer diaSemana;
    private Integer periodoOrden;
    private Long materiaId;
    private Long docenteId;
    private Long aulaId;
}
