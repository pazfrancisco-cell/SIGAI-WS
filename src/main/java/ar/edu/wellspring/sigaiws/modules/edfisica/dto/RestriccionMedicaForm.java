package ar.edu.wellspring.sigaiws.modules.edfisica.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class RestriccionMedicaForm {
    @NotNull(message = "Seleccione el alumno")
    private Long alumnoId;
    @NotBlank(message = "Ingrese la descripcion de la restriccion")
    private String descripcion;
    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaInicio = LocalDate.now();
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaFin;
}
