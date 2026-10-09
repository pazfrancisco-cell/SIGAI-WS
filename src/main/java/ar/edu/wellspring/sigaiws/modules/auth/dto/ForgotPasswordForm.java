package ar.edu.wellspring.sigaiws.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordForm {

    @NotBlank
    @Email(message = "Ingrese un email valido")
    private String email;
}
