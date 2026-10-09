package ar.edu.wellspring.sigaiws.modules.edfisica.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Alta de una prueba fisica nueva en el catalogo (nombre + unidad + direccion de mejora). */
@Getter
@Setter
public class PruebaFisicaDefinicionForm {
    @NotBlank(message = "Ingrese el nombre de la prueba")
    private String nombre;
    @NotBlank(message = "Ingrese la unidad de medida")
    private String unidad;
    private boolean mayorEsMejor = true;
}
