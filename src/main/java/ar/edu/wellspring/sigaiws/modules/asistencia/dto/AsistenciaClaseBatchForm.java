package ar.edu.wellspring.sigaiws.modules.asistencia.dto;

import ar.edu.wellspring.sigaiws.modules.asistencia.domain.TipoAsistenciaClase;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Seccion 5.2.6: toma de asistencia por clase del docente. */
@Getter
@Setter
public class AsistenciaClaseBatchForm {

    private Long materiaId;
    private Long cursoDivisionId;
    private Long horarioSlotId;
    private LocalDate fecha = LocalDate.now();
    private List<Item> items = new ArrayList<>();

    @Getter
    @Setter
    public static class Item {
        private Long alumnoId;
        private TipoAsistenciaClase tipo = TipoAsistenciaClase.PRESENTE;
        private String observacion;
    }
}
