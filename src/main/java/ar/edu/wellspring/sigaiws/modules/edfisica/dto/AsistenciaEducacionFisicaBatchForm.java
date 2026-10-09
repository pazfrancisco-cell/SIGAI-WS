package ar.edu.wellspring.sigaiws.modules.edfisica.dto;

import ar.edu.wellspring.sigaiws.modules.edfisica.domain.TipoAsistenciaEducacionFisica;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/** Seccion 5.7 (extension): toma de asistencia diaria de un grupo de Educacion Fisica. */
@Getter
@Setter
public class AsistenciaEducacionFisicaBatchForm {

    private Long grupoId;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha = LocalDate.now();
    private List<Item> items = new ArrayList<>();

    @Getter
    @Setter
    public static class Item {
        private Long alumnoId;
        private TipoAsistenciaEducacionFisica tipo = TipoAsistenciaEducacionFisica.PRESENTE;
    }
}
