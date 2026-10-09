package ar.edu.wellspring.sigaiws.modules.configuracion.web;

import ar.edu.wellspring.sigaiws.modules.configuracion.dto.ConfiguracionSmtpForm;
import ar.edu.wellspring.sigaiws.modules.configuracion.service.ConfiguracionSmtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/configuracion/smtp")
@PreAuthorize("hasRole('SUPERADMIN')")
@RequiredArgsConstructor
public class ConfiguracionSmtpController {

    private final ConfiguracionSmtpService service;

    @GetMapping
    public String form(Model model) {
        var config = service.obtener();
        ConfiguracionSmtpForm form = new ConfiguracionSmtpForm();
        form.setUsarConfiguracionPersonalizada(config.isUsarConfiguracionPersonalizada());
        form.setHost(config.getHost());
        form.setPuerto(config.getPuerto());
        form.setUsuario(config.getUsuario());
        form.setRemitente(config.getRemitente());
        form.setAuthHabilitado(config.isAuthHabilitado());
        form.setStarttlsHabilitado(config.isStarttlsHabilitado());
        model.addAttribute("configuracionSmtpForm", form);
        model.addAttribute("passwordYaConfigurada", config.getPassword() != null);
        return "configuracion/smtp";
    }

    @PostMapping
    public String guardar(@ModelAttribute ConfiguracionSmtpForm form, RedirectAttributes redirectAttributes) {
        service.guardar(form);
        redirectAttributes.addFlashAttribute("mensajeExito", "Configuracion SMTP guardada correctamente.");
        return "redirect:/configuracion/smtp";
    }
}
