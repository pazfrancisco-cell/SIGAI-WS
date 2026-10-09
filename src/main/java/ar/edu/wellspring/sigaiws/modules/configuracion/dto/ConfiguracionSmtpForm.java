package ar.edu.wellspring.sigaiws.modules.configuracion.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Formulario de configuracion SMTP (seccion 5.14.2). El campo password llega
 * vacio al mostrar el formulario; si se envia en blanco al guardar, se
 * conserva la contrasena ya almacenada (ver ConfiguracionSmtpService.guardar).
 */
@Getter
@Setter
public class ConfiguracionSmtpForm {

    private boolean usarConfiguracionPersonalizada;
    private String host;
    private Integer puerto;
    private String usuario;
    private String password;
    private String remitente;
    private boolean authHabilitado = true;
    private boolean starttlsHabilitado = true;
}
