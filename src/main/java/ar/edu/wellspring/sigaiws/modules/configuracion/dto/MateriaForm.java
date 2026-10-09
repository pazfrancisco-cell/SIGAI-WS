package ar.edu.wellspring.sigaiws.modules.configuracion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MateriaForm {
    private Long id;

    @NotBlank
    @Size(max = 120)
    private String nombre;

    @NotEmpty(message = "Seleccione al menos un curso/division")
    private Set<Long> cursoDivisionIds;

    private boolean activa = true;
}
