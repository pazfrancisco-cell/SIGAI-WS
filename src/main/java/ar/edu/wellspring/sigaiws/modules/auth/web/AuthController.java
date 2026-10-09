package ar.edu.wellspring.sigaiws.modules.auth.web;

import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.modules.auth.dto.ChangePasswordForm;
import ar.edu.wellspring.sigaiws.modules.auth.dto.ForgotPasswordForm;
import ar.edu.wellspring.sigaiws.modules.auth.dto.ResetPasswordForm;
import ar.edu.wellspring.sigaiws.modules.auth.security.SigaiUserPrincipal;
import ar.edu.wellspring.sigaiws.modules.auth.service.AuthService;
import ar.edu.wellspring.sigaiws.modules.auth.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                         @RequestParam(required = false) String logout,
                         Model model) {
        if (error != null) {
            model.addAttribute("error", error); // "credenciales" o "bloqueado" (ver SecurityConfig)
        }
        model.addAttribute("logout", logout != null);
        return "auth/login";
    }

    @GetMapping("/cambiar-password")
    public String cambiarPasswordForm(Model model) {
        model.addAttribute("changePasswordForm", new ChangePasswordForm());
        return "auth/cambiar-password";
    }

    @PostMapping("/cambiar-password")
    public String cambiarPassword(@Valid @ModelAttribute ChangePasswordForm form, BindingResult bindingResult,
                                   @AuthenticationPrincipal SigaiUserPrincipal principal,
                                   HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/cambiar-password";
        }
        try {
            authService.cambiarPassword(principal.getId(), form.getPasswordActual(),
                    form.getPasswordNueva(), form.getPasswordConfirmacion());
        } catch (BusinessException e) {
            bindingResult.rejectValue(campoDelErrorCambiar(e.getMessage()), "invalido", e.getMessage());
            return "auth/cambiar-password";
        }
        // Se invalida la sesion para forzar un nuevo login con la contraseña actualizada.
        request.getSession().invalidate();
        redirectAttributes.addFlashAttribute("mensajeExito", "Contraseña actualizada. Inicia sesion nuevamente.");
        return "redirect:/login";
    }

    /** Mapea el mensaje de AuthService.cambiarPassword al campo del formulario que hay que resaltar. */
    private String campoDelErrorCambiar(String mensaje) {
        if (mensaje.contains("actual")) {
            return "passwordActual";
        }
        if (mensaje.contains("coinciden")) {
            return "passwordConfirmacion";
        }
        return "passwordNueva";
    }

    @GetMapping("/olvide-password")
    public String olvidePasswordForm(Model model) {
        model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        return "auth/olvide-password";
    }

    @PostMapping("/olvide-password")
    public String olvidePassword(@Valid @ModelAttribute ForgotPasswordForm form, BindingResult bindingResult,
                                  Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/olvide-password";
        }
        passwordResetService.solicitar(form.getEmail());
        model.addAttribute("enviado", true);
        return "auth/olvide-password";
    }

    @GetMapping("/restablecer-password")
    public String restablecerPasswordForm(@RequestParam String token, Model model) {
        ResetPasswordForm form = new ResetPasswordForm();
        form.setToken(token);
        model.addAttribute("resetPasswordForm", form);
        return "auth/restablecer-password";
    }

    @PostMapping("/restablecer-password")
    public String restablecerPassword(@Valid @ModelAttribute ResetPasswordForm form, BindingResult bindingResult,
                                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/restablecer-password";
        }
        try {
            passwordResetService.restablecer(form.getToken(), form.getPasswordNueva(), form.getPasswordConfirmacion());
        } catch (BusinessException e) {
            String campo = campoDelErrorRestablecer(e.getMessage());
            if (campo == null) {
                throw e; // enlace invalido/vencido: no hay campo que corregir, se maneja como error general
            }
            bindingResult.rejectValue(campo, "invalido", e.getMessage());
            return "auth/restablecer-password";
        }
        redirectAttributes.addFlashAttribute("mensajeExito", "Contraseña restablecida. Ya podes iniciar sesion.");
        return "redirect:/login";
    }

    /** Mapea el mensaje de PasswordResetService.restablecer al campo del formulario que hay que resaltar. */
    private String campoDelErrorRestablecer(String mensaje) {
        if (mensaje.contains("coinciden")) {
            return "passwordConfirmacion";
        }
        if (mensaje.contains("enlace")) {
            return null;
        }
        return "passwordNueva";
    }
}
