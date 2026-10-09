package ar.edu.wellspring.sigaiws.modules.calificacion.dto;

import ar.edu.wellspring.sigaiws.modules.calificacion.domain.TipoEvaluacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/** Seccion 5.3.4: modal "Nueva Evaluacion". */
@Getter
@Setter
public class EvaluacionForm {

    @NotNull
    private Long materiaId;
    @NotNull
    private Long cursoDivisionId;
    @NotNull
    private Integer cuatrimestre;
    @NotNull(message = "Seleccione el tipo de evaluacion")
    private TipoEvaluacion tipo;
    @NotBlank(message = "Ingrese un titulo")
    private String titulo;
    @NotNull(message = "Ingrese la fecha")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;
}
