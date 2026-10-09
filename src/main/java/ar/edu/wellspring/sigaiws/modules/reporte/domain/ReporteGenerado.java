package ar.edu.wellspring.sigaiws.modules.reporte.domain;

import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** Modulo 5.8. Metadata de cada PDF generado; el contenido vive en el filesystem. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reporte_generado")
public class ReporteGenerado {

    public enum Tipo { BOLETIN, ASISTENCIA, FIP, RITE, DISCIPLINARIO_CURSO, RENDIMIENTO_MATERIA, ASISTENCIA_CURSO, CONDICION_LIBRE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 40)
    private Tipo tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id")
    private Alumno alumno;

    @Column(name = "cuatrimestre")
    private Integer cuatrimestre;

    @Column(name = "path_archivo", nullable = false, length = 400)
    private String pathArchivo;

    /** Solo para FIP: observaciones pedagogicas cargadas a mano al generarla. */
    @Column(name = "observaciones", columnDefinition = "NVARCHAR(MAX)")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generado_por_id", nullable = false)
    private Usuario generadoPor;

    @Column(name = "generado_en", nullable = false)
    private LocalDateTime generadoEn = LocalDateTime.now();
}
