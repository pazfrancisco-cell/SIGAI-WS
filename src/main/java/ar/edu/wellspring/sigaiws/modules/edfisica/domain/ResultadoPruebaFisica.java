package ar.edu.wellspring.sigaiws.modules.edfisica.domain;

import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "resultado_prueba_fisica")
public class ResultadoPruebaFisica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prueba_id", nullable = false)
    private PruebaFisica prueba;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    /** Null cuando {@link #saf} es true (el alumno no rindio ese dia). */
    @Column(name = "valor")
    private BigDecimal valor;

    /** Seccion 5.7 (extension): "Sin Apto Fisico" solo para esta sesion puntual (ej. lesion), sin tocar el apto fisico anual. */
    @Column(name = "saf", nullable = false)
    private boolean saf = false;
}
