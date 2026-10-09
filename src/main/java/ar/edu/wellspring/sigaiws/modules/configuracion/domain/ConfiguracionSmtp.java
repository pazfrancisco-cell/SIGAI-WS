package ar.edu.wellspring.sigaiws.modules.configuracion.domain;

import ar.edu.wellspring.sigaiws.common.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Seccion 5.14.2: configuracion del servidor SMTP institucional, editable por
 * el Superadmin. Fila unica (id=1, sembrada por la migracion V16). Si
 * usarConfiguracionPersonalizada esta apagado, EmailService usa el
 * JavaMailSender autoconfigurado por Spring Boot (spring.mail.* del .env).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "configuracion_smtp")
public class ConfiguracionSmtp {

    @Id
    private Long id;

    @Column(name = "usar_configuracion_personalizada", nullable = false)
    private boolean usarConfiguracionPersonalizada;

    @Column(name = "host", length = 200)
    private String host;

    @Column(name = "puerto")
    private Integer puerto;

    @Column(name = "usuario", length = 200)
    private String usuario;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "password_cifrada", length = 256)
    private String password;

    @Column(name = "remitente", length = 200)
    private String remitente;

    @Column(name = "auth_habilitado", nullable = false)
    private boolean authHabilitado = true;

    @Column(name = "starttls_habilitado", nullable = false)
    private boolean starttlsHabilitado = true;
}
