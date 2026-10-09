package ar.edu.wellspring.sigaiws.modules.asistencia.dto;

import ar.edu.wellspring.sigaiws.modules.asistencia.domain.JustificacionInasistencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class JustificacionForm {

    @NotNull
    private Long asistenciaJornadaId;

    @NotNull(message = "Seleccione el tipo de justificacion")
    private JustificacionInasistencia.Tipo tipo;

    @NotBlank(message = "Ingrese una descripcion")
    @Size(max = 200)
    private String descripcion;

    /** Seccion 5.2.8: "puede adjuntar un documento de soporte" (opcional). */
    private MultipartFile documento;
}
