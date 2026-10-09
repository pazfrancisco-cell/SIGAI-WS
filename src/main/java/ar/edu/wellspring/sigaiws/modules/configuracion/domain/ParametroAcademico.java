package ar.edu.wellspring.sigaiws.modules.configuracion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fila unica (seccion 5.14.2). Valores por defecto sembrados en V13; editables
 * por el Superadmin desde Configuracion Institucional.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "parametro_academico")
public class ParametroAcademico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "umbral_falta_1", nullable = false)
    private Integer umbralFalta1 = 10;

    @Column(name = "umbral_falta_2", nullable = false)
    private Integer umbralFalta2 = 15;

    @Column(name = "umbral_falta_3", nullable = false)
    private Integer umbralFalta3 = 20;

    @Column(name = "umbral_falta_4_condicion_libre", nullable = false)
    private Integer umbralFalta4CondicionLibre = 28;

    @Column(name = "nota_minima_aprobacion", nullable = false)
    private BigDecimal notaMinimaAprobacion = new BigDecimal("7.0");

    @Column(name = "umbral_bajo_rendimiento", nullable = false)
    private BigDecimal umbralBajoRendimiento = new BigDecimal("6.0");

    @Column(name = "porcentaje_asistencia_materia", nullable = false)
    private BigDecimal porcentajeAsistenciaMateria = new BigDecimal("75.00");

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @Column(name = "actualizado_por", length = 120)
    private String actualizadoPor;
}
