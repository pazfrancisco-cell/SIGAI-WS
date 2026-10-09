package ar.edu.wellspring.sigaiws.modules.edfisica.domain;

import ar.edu.wellspring.sigaiws.common.crypto.EncryptedStringConverter;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Seccion 5.7.5. Descripcion cifrada por tratarse de datos de salud (seccion 7.2). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "restriccion_medica")
public class RestriccionMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "descripcion_cifrada", nullable = false, length = 2048)
    private String descripcion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "documento_path", length = 400)
    private String documentoPath;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por_id", nullable = false)
    private Usuario creadoPor;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    public boolean isActiva() {
        LocalDate hoy = LocalDate.now();
        return !hoy.isBefore(fechaInicio) && (fechaFin == null || !hoy.isAfter(fechaFin));
    }
}
