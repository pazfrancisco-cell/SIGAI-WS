package ar.edu.wellspring.sigaiws.modules.mensajeria.domain;

import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CursoDivision;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "comunicado_destinatario")
public class ComunicadoDestinatario {

    public enum TipoDestinatario { TODOS_PADRES, TODOS_DOCENTES, CURSO, ALUMNO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comunicado_id", nullable = false)
    private ComunicadoInstitucional comunicado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_destinatario", nullable = false, length = 30)
    private TipoDestinatario tipoDestinatario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_division_id")
    private CursoDivision cursoDivision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id")
    private Alumno alumno;
}
