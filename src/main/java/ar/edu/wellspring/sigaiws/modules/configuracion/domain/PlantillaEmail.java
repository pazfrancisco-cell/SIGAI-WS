package ar.edu.wellspring.sigaiws.modules.configuracion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Seccion 5.14.2: plantillas editables de los emails automaticos del sistema,
 * con variables dinamicas entre llaves (ej. {nombre}) sustituidas en tiempo
 * de envio por PlantillaEmailService.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "plantilla_email")
public class PlantillaEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo", nullable = false, unique = true, length = 60)
    private String tipo;

    @Column(name = "descripcion", nullable = false, length = 200)
    private String descripcion;

    @Column(name = "asunto", nullable = false, length = 200)
    private String asunto;

    @Lob
    @Column(name = "cuerpo", nullable = false)
    private String cuerpo;

    @Column(name = "habilitada", nullable = false)
    private boolean habilitada = true;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();
}
