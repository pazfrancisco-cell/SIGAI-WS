package ar.edu.wellspring.sigaiws.modules.mensajeria.domain;

import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Seccion 5.10.2 (Comunicados Institucionales): difusion unidireccional, no admiten respuesta. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "comunicado_institucional")
public class ComunicadoInstitucional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asunto", nullable = false, length = 120)
    private String asunto;

    @Lob
    @Column(name = "contenido", nullable = false)
    private String contenido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(name = "enviar_email", nullable = false)
    private boolean enviarEmail = false;

    @Column(name = "adjunto_path", length = 400)
    private String adjuntoPath;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
