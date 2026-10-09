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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Seccion 5.1.8 (Flujo: Pase de Alumno). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pase_alumno")
public class PaseAlumno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "institucion_destino", nullable = false, length = 200)
    private String institucionDestino;

    @Column(name = "pdf_path", length = 400)
    private String pdfPath;

    @Column(name = "generado_en", nullable = false)
    private LocalDateTime generadoEn = LocalDateTime.now();

    @Column(name = "generado_por", length = 120)
    private String generadoPor;
}
