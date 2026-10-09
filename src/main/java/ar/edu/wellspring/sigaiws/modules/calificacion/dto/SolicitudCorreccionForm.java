package ar.edu.wellspring.sigaiws.modules.calificacion.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Seccion 5.3.6. */
@Getter
@Setter
public class SolicitudCorreccionForm {

    @NotNull
    private Long notaId;

    @NotNull(message = "Ingrese la nueva nota")
    @DecimalMin("1.0") @DecimalMax("10.0")
    private BigDecimal notaPropuesta;

    @Size(min = 20, max = 500, message = "La justificacion debe tener al menos 20 caracteres")
    private String justificacion;
}
