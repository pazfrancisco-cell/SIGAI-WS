package ar.edu.wellspring.sigaiws.modules.disciplina.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/** Seccion 5.4.4. */
@Getter
@Setter
public class ObservacionForm {

    @NotNull(message = "Seleccione el alumno")
    private Long alumnoId;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha = LocalDate.now();

    @Size(min = 20, max = 500, message = "La descripcion debe tener entre 20 y 500 caracteres")
    private String descripcion;

    private String categoria;
    private String firmantesAdicionales;

    /** Gravedad de la sancion: cuantas observaciones equivalentes registrar de una sola vez. */
    @NotNull
    @Min(1)
    @Max(20)
    private Integer cantidad = 1;
}
