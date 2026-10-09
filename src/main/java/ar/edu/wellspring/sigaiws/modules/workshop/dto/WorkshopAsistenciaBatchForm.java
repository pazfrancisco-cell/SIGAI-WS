package ar.edu.wellspring.sigaiws.modules.workshop.dto;

import ar.edu.wellspring.sigaiws.modules.asistencia.domain.TipoAsistenciaClase;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Fase 4: toma de asistencia de un workshop, clase por clase (roster dinamico segun cuatrimestre). */
@Getter
@Setter
public class WorkshopAsistenciaBatchForm {

    private Long workshopId;
    private Integer cuatrimestre;
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
