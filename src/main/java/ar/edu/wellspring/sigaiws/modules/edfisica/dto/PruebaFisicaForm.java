package ar.edu.wellspring.sigaiws.modules.edfisica.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class PruebaFisicaForm {
    private Long grupoId;
    private Integer cuatrimestre;
    @NotNull(message = "Seleccione la prueba fisica")
    private Long definicionId;
    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha = LocalDate.now();
    private List<ResultadoItem> resultados = new ArrayList<>();

    @Getter
    @Setter
    public static class ResultadoItem {
        private Long alumnoId;
        private String valor;
        private boolean saf;
    }
}
