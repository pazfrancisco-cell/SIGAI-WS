package ar.edu.wellspring.sigaiws.modules.configuracion.dto;

import ar.edu.wellspring.sigaiws.modules.configuracion.domain.PeriodoHorario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

/** Seccion 5.14.2: "Configuracion de la distribucion del dia escolar: periodos, horarios, recreos y almuerzo." */
@Getter
@Setter
public class PeriodoHorarioForm {

    private Long id;

    @NotNull(message = "El orden es obligatorio")
    private Integer orden;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;

    @NotNull(message = "Seleccione el tipo")
    private PeriodoHorario.Tipo tipo = PeriodoHorario.Tipo.CLASE;
}
