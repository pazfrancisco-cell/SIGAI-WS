package ar.edu.wellspring.sigaiws.common.notification;

import ar.edu.wellspring.sigaiws.modules.configuracion.domain.ConfiguracionSmtp;
import ar.edu.wellspring.sigaiws.modules.configuracion.service.ConfiguracionSmtpService;
import ar.edu.wellspring.sigaiws.modules.configuracion.service.PlantillaEmailService;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Envio de notificaciones por SMTP (seccion 3.2: "sin dependencia de
 * servicios externos de terceros"). Usado por: bienvenida de usuario (6.1),
 * recuperacion de contraseña (7.1), y las notificaciones automaticas de
 * 5.10.2/5.11.4/5.13.2. Por defecto usa el JavaMailSender autoconfigurado por
 * Spring Boot a partir de spring.mail.* (.env); si el Superadmin activa
 * "Usar configuracion personalizada" en Configuracion SMTP (seccion 5.14.2),
 * se arma un JavaMailSender ad-hoc con esos datos.
 *
 * El envio es @Async para no bloquear el hilo de la request HTTP; un fallo de
 * SMTP se loguea pero no debe hacer fallar la operacion de negocio que origino
 * el correo (ej. el alta de un alumno no debe fallar porque el SMTP este caido).
 */
@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSenderPorDefecto;
    private final ConfiguracionSmtpService configuracionSmtpService;
    private final PlantillaEmailService plantillaEmailService;
    private final String remitentePorDefecto;

    public EmailService(JavaMailSender mailSenderPorDefecto, ConfiguracionSmtpService configuracionSmtpService,
                         PlantillaEmailService plantillaEmailService,
                         @Value("${sigai.email.remitente-por-defecto}") String remitentePorDefecto) {
        this.mailSenderPorDefecto = mailSenderPorDefecto;
        this.configuracionSmtpService = configuracionSmtpService;
        this.plantillaEmailService = plantillaEmailService;
        this.remitentePorDefecto = remitentePorDefecto;
    }

    public void enviarConPlantilla(String destinatario, String tipo, Map<String, String> variables) {
        plantillaEmailService.renderizar(tipo, variables)
                .ifPresent(r -> enviarHtml(destinatario, r.asunto(), r.cuerpo()));
    }

    @Async
    public void enviarHtml(String destinatario, String asunto, String cuerpoHtml) {
        ConfiguracionSmtp config = configuracionSmtpService.obtener();
        JavaMailSender sender = config.isUsarConfiguracionPersonalizada() ? construirSender(config) : mailSenderPorDefecto;
        String remitente = config.isUsarConfiguracionPersonalizada() && config.getRemitente() != null
                ? config.getRemitente() : remitentePorDefecto;
        try {
            MimeMessage mensaje = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);
            sender.send(mensaje);
        } catch (Exception e) {
            log.error("No se pudo enviar el email a {} (asunto: {})", destinatario, asunto, e);
        }
    }

    private JavaMailSender construirSender(ConfiguracionSmtp config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getHost());
        if (config.getPuerto() != null) {
            sender.setPort(config.getPuerto());
        }
        sender.setUsername(config.getUsuario());
        sender.setPassword(config.getPassword());
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", config.isAuthHabilitado());
        props.put("mail.smtp.starttls.enable", config.isStarttlsHabilitado());
        return sender;
    }
}
