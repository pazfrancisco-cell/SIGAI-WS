package ar.edu.wellspring.sigaiws.modules.workshop.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Fase 4: carga de notas de un workshop (texto libre por alumno, sin escala numerica). */
@Getter
@Setter
public class WorkshopCargaNotasForm {

    private Long evaluacionId;
    private List<Item> items = new ArrayList<>();

    @Getter
    @Setter
    public static class Item {
        private Long alumnoId;
        private String texto;
    }
}
