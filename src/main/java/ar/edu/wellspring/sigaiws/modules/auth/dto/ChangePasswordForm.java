package ar.edu.wellspring.sigaiws.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordForm {

    @NotBlank(message = "Ingrese su contraseña actual")
    private String passwordActual;

    @NotBlank(message = "Ingrese la nueva contraseña")
    private String passwordNueva;

    @NotBlank(message = "Confirme la nueva contraseña")
    private String passwordConfirmacion;
}
