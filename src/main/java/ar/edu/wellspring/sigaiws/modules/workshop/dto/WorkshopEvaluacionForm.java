package ar.edu.wellspring.sigaiws.modules.workshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/** Fase 4: "+ Nueva Evaluacion" de un workshop (sin tipo, es a texto libre). */
@Getter
@Setter
public class WorkshopEvaluacionForm {

    @NotNull
    private Long workshopId;
    @NotNull
    private Integer cuatrimestre;
    @NotBlank(message = "Ingrese un titulo")
    private String titulo;
    @NotNull(message = "Ingrese la fecha")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;
}
