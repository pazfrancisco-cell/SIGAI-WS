package ar.edu.wellspring.sigaiws.modules.configuracion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AulaForm {
    private Long id;

    @NotBlank
    private String nombre;

    @Positive
    private Integer capacidad;

    private boolean activa = true;
}
