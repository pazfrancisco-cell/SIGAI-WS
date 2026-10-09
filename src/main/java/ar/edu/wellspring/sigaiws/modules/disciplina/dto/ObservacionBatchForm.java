package ar.edu.wellspring.sigaiws.modules.disciplina.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Seccion 5.4.4: carga de una o mas observaciones en un unico envio. */
@Getter
@Setter
public class ObservacionBatchForm {

    @Valid
    @Size(min = 1, message = "Agregue al menos una observacion")
    private List<ObservacionForm> observaciones = new ArrayList<>();
}
