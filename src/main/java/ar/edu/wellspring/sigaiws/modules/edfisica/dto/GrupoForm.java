package ar.edu.wellspring.sigaiws.modules.edfisica.dto;

import ar.edu.wellspring.sigaiws.common.enums.Genero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GrupoForm {
    private Long id;
    @NotBlank
    private String nombre;
    @NotNull
    private Genero genero;
    @NotNull(message = "Seleccione el docente")
    private Long docenteId;
    @NotNull
    private Integer cuatrimestre = 1;
    private List<Long> alumnoIds = new ArrayList<>();

    /** Pruebas del catalogo ya existentes que se asignan a este grupo. */
    private List<Long> definicionIds = new ArrayList<>();

    /** Pruebas nuevas para el catalogo, creadas al vuelo desde este mismo formulario. */
    private List<PruebaFisicaDefinicionForm> nuevasDefiniciones = new ArrayList<>();
}
