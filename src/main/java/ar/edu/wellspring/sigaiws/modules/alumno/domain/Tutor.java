package ar.edu.wellspring.sigaiws.modules.alumno.domain;

import ar.edu.wellspring.sigaiws.common.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Seccion 5.1.5. Puede tener cuenta de acceso al Portal de Padres (5.11);
 * usuarioId se completa cuando se genera esa cuenta (seccion 6.1, paso 6),
 * fuera del alcance de esta fase (el Portal de Padres es Fase 6 del roadmap).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tutor")
public class Tutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "apellido", nullable = false, length = 80)
    private String apellido;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "dni_cifrado", nullable = false, length = 256)
    private String dni;

    @Column(name = "dni_hash", nullable = false, unique = true, length = 64)
    private String dniHash;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "telefono_cifrado", nullable = false, length = 256)
    private String telefono;

    @Column(name = "email", nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "usuario_id", unique = true)
    private Long usuarioId;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    public String getNombreCompleto() {
        return apellido + ", " + nombre;
    }
}
