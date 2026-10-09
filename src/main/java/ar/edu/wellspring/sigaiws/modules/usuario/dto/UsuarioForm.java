package ar.edu.wellspring.sigaiws.modules.usuario.dto;

import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO de alta/edicion (seccion 5.12.4). Deliberadamente separado de la entidad
 * Usuario para no permitir binding directo sobre campos sensibles (passwordHash,
 * intentosFallidos, etc.) desde un formulario HTML.
 */
@Getter
@Setter
public class UsuarioForm {

    private Long id;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 160)
    private String nombreCompleto;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato valido")
    @Size(max = 180)
    private String email;

    @NotEmpty(message = "Debe seleccionar al menos un rol")
    private Set<RolNombre> roles;

    private boolean activo = true;

    private boolean esDocenteEducacionFisica = false;
    private boolean esDocenteWorkshop = false;
}
