package ar.edu.wellspring.sigaiws.modules.configuracion.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Seccion 5.14.2: umbrales configurables (valores por defecto: 10/15/20/28, nota minima 7, bajo rendimiento 6, asistencia 75%). */
@Getter
@Setter
public class ParametroAcademicoForm {

    @NotNull @Min(1)
    private Integer umbralFalta1;
    @NotNull @Min(1)
    private Integer umbralFalta2;
    @NotNull @Min(1)
    private Integer umbralFalta3;
    @NotNull @Min(1)
    private Integer umbralFalta4CondicionLibre;

    @NotNull @DecimalMin("1.0") @DecimalMax("10.0")
    private BigDecimal notaMinimaAprobacion;

    @NotNull @DecimalMin("1.0") @DecimalMax("10.0")
    private BigDecimal umbralBajoRendimiento;

    @NotNull @DecimalMin("0.0") @DecimalMax("100.0")
    private BigDecimal porcentajeAsistenciaMateria;
}
