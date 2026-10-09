package ar.edu.wellspring.sigaiws.modules.mensajeria.domain;

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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Seccion 5.10.2: fecha y hora en que un destinatario abrio un comunicado por primera vez. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "comunicado_lectura")
public class ComunicadoLectura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comunicado_id", nullable = false)
    private ComunicadoInstitucional comunicado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "leido_en", nullable = false)
    private LocalDateTime leidoEn = LocalDateTime.now();
}
