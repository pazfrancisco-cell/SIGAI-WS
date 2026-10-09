package ar.edu.wellspring.sigaiws.modules.alumno.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** Seccion 5.1.8: cambiar estado a "Inactivo - Pase", registrar fecha e institucion destino. */
@Getter
@Setter
public class PaseAlumnoForm {

    @NotNull
    private LocalDate fecha = LocalDate.now();

    @NotBlank(message = "Indique la institucion de destino")
    private String institucionDestino;
}
