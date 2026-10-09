package ar.edu.wellspring.sigaiws.modules.asistencia.domain;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Seccion 5.2.8: no elimina la falta, cambia su tipo a AUSENTE_JUSTIFICADO. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "justificacion_inasistencia")
public class JustificacionInasistencia {

    public enum Tipo { MEDICA, FAMILIAR, INSTITUCIONAL, OTRA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asistencia_jornada_id", nullable = false, unique = true)
    private AsistenciaJornada asistenciaJornada;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private Tipo tipo;

    @Column(name = "descripcion", nullable = false, length = 200)
    private String descripcion;

    @Column(name = "documento_path", length = 400)
    private String documentoPath;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "justificado_por_id", nullable = false)
    private Usuario justificadoPor;

    @Column(name = "justificado_en", nullable = false)
    private LocalDateTime justificadoEn = LocalDateTime.now();
}
