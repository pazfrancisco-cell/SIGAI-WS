package ar.edu.wellspring.sigaiws.modules.horario.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AsignacionDocenteForm {

    @NotNull(message = "Seleccione el docente")
    private Long docenteId;

    @NotNull(message = "Seleccione la materia")
    private Long materiaId;

    @NotNull(message = "Seleccione el curso/division")
    private Long cursoDivisionId;
}
