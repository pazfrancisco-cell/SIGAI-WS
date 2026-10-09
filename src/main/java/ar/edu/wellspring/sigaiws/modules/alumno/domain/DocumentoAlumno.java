package ar.edu.wellspring.sigaiws.modules.alumno.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Seccion 5.1.5 (Documentos adicionales) / tab "Documentos" del legajo.
 * pathArchivo usa un nombre de archivo aleatorio en disco (seccion 7.2:
 * "no predecible por enumeracion"), generado por AlmacenamientoService.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "documento_alumno")
public class DocumentoAlumno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @Column(name = "tipo", nullable = false, length = 40)
    private String tipo;

    @Column(name = "nombre_archivo", nullable = false, length = 260)
    private String nombreArchivo;

    @Column(name = "path_archivo", nullable = false, length = 400)
    private String pathArchivo;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Column(name = "subido_en", nullable = false)
    private LocalDateTime subidoEn = LocalDateTime.now();

    @Column(name = "subido_por", length = 120)
    private String subidoPor;
}
