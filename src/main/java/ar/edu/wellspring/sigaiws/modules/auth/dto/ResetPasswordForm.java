package ar.edu.wellspring.sigaiws.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordForm {

    @NotBlank
    private String token;

    @NotBlank(message = "Ingrese la nueva contraseña")
    private String passwordNueva;

    @NotBlank(message = "Confirme la nueva contraseña")
    private String passwordConfirmacion;
}
