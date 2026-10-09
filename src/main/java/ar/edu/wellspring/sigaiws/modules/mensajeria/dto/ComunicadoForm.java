package ar.edu.wellspring.sigaiws.modules.mensajeria.dto;

import ar.edu.wellspring.sigaiws.modules.mensajeria.domain.ComunicadoDestinatario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/** Seccion 5.10.4. "Destinatarios*: selector multiple" (TODOS_PADRES, TODOS_DOCENTES, CURSO, ALUMNO combinables). */
@Getter
@Setter
public class ComunicadoForm {

    @NotEmpty(message = "Seleccione al menos un destinatario")
    private List<ComunicadoDestinatario.TipoDestinatario> tiposDestinatario;

    private Long cursoDivisionId;
    private Long alumnoId;

    @NotBlank(message = "Ingrese un asunto")
    private String asunto;

    @NotBlank(message = "Ingrese el contenido")
    private String contenido;

    private boolean enviarEmail = false;

    private MultipartFile adjunto;
}
