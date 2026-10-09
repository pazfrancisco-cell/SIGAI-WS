package ar.edu.wellspring.sigaiws.modules.configuracion.dto;

import ar.edu.wellspring.sigaiws.modules.configuracion.domain.WorkshopBanda;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkshopForm {
    private Long id;

    @NotBlank
    @Size(max = 120)
    private String nombre;

    @Size(max = 500)
    private String descripcion;

    @NotNull(message = "Seleccione la banda de años")
    private WorkshopBanda banda;

    private boolean activo = true;
}
