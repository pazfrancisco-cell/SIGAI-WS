package ar.edu.wellspring.sigaiws.modules.asistencia.dto;

import ar.edu.wellspring.sigaiws.modules.asistencia.domain.AsistenciaJornada;
import ar.edu.wellspring.sigaiws.modules.asistencia.domain.TipoAsistenciaJornada;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/** Seccion 5.2.5: toma de asistencia protocolar del preceptor, para todo un curso/turno a la vez. */
@Getter
@Setter
public class AsistenciaJornadaBatchForm {

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha = LocalDate.now();
    private AsistenciaJornada.Turno turno;
    private Long cursoDivisionId;
    private List<Item> items = new ArrayList<>();

    @Getter
    @Setter
    public static class Item {
        private Long alumnoId;
        private TipoAsistenciaJornada tipo = TipoAsistenciaJornada.PRESENTE;
    }
}
