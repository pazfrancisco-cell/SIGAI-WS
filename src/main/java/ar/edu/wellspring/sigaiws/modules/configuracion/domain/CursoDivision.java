package ar.edu.wellspring.sigaiws.modules.configuracion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Curso/division del nivel secundario (seccion 1.4: años 1-6). Se gestionan
 * desde Configuracion; uno con historial se desactiva en vez de borrarse.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"anio", "division"})
@Entity
@Table(name = "curso_division")
public class CursoDivision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "division", nullable = false, length = 1)
    private String division;

    /** Un curso inactivo no aparece para cargas nuevas, pero sus datos historicos se siguen viendo. */
    @Column(name = "activa", nullable = false)
    private boolean activa = true;

    public CursoDivision(Long id, Integer anio, String division) {
        this.id = id;
        this.anio = anio;
        this.division = division;
    }

    public String getNombreCompleto() {
        return anio + "° " + division;
    }
}
