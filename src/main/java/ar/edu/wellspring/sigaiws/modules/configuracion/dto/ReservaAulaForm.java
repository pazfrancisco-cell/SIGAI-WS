package ar.edu.wellspring.sigaiws.modules.configuracion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/** Seccion 5.5.1: reserva puntual de un aula por un docente. */
@Getter
@Setter
public class ReservaAulaForm {

    @NotNull
    private Long aulaId;

    @NotNull(message = "Seleccione el curso/division")
    private Long cursoDivisionId;

    @NotNull(message = "Ingrese la fecha")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    @NotNull(message = "Seleccione el periodo")
    private Integer periodoOrden;

    @NotBlank(message = "Indique para que va a usar el aula")
    private String motivo;
}
