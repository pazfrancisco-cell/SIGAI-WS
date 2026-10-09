package ar.edu.wellspring.sigaiws.modules.calificacion.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Seccion 5.3.4: tabla de calificaciones (filas=alumnos, columnas=evaluaciones), guardado en bloque. */
@Getter
@Setter
public class CargaNotasForm {

    private Long evaluacionId;
    private List<Item> items = new ArrayList<>();

    @Getter
    @Setter
    public static class Item {
        private Long alumnoId;
        private String valor; // string para permitir vacio ("-"), se valida/convierte en el service
    }
}
