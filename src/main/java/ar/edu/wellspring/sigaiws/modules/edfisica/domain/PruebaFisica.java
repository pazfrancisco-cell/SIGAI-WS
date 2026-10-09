package ar.edu.wellspring.sigaiws.modules.edfisica.domain;

import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Seccion 5.7.4: sesion fechada de una prueba fisica del catalogo ({@link PruebaFisicaDefinicion}), tomada a un grupo. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "prueba_fisica")
public class PruebaFisica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grupo_id", nullable = false)
    private GrupoEducacionFisica grupo;

    @Column(name = "cuatrimestre", nullable = false)
    private Integer cuatrimestre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "definicion_id", nullable = false)
    private PruebaFisicaDefinicion definicion;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por_id", nullable = false)
    private Usuario creadoPor;
}
