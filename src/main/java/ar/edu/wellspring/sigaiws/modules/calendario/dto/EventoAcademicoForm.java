package ar.edu.wellspring.sigaiws.modules.calendario.dto;

import ar.edu.wellspring.sigaiws.modules.calendario.domain.TipoEventoAcademico;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class EventoAcademicoForm {

    private Long id;

    @NotBlank(message = "Ingrese un titulo")
    private String titulo;

    private String descripcion;

    @NotNull(message = "Seleccione el tipo de evento")
    private TipoEventoAcademico tipo;

    @NotNull(message = "Ingrese la fecha de inicio")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaInicio;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaFin;

    /** Null = evento institucional, visible para toda la institucion. */
    private Long cursoDivisionId;
}
