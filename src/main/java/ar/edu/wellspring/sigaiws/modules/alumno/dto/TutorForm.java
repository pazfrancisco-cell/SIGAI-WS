package ar.edu.wellspring.sigaiws.modules.alumno.dto;

import ar.edu.wellspring.sigaiws.modules.alumno.domain.VinculoTutor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/** Seccion 5.1.5, Seccion 3 - Tutores/Responsables (hasta 2 por alumno). */
@Getter
@Setter
public class TutorForm {

    @NotBlank(message = "El apellido del tutor es obligatorio")
    private String apellido;

    @NotBlank(message = "El nombre del tutor es obligatorio")
    private String nombre;

    @NotNull(message = "Seleccione el vinculo")
    private VinculoTutor vinculo;

    @NotBlank(message = "El DNI del tutor es obligatorio")
    @Pattern(regexp = "\\d{7,8}", message = "El DNI debe tener 7 u 8 digitos")
    private String dni;

    @NotBlank(message = "El telefono es obligatorio")
    private String telefono;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato valido")
    private String email;

    private boolean recibeNotificaciones = true;
}
