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

/**
 * Seccion 5.10.2 / 5.11.4: notificacion personal de un usuario (no respondible).
 * A diferencia de las Alertas (modulo 5.13, institucionales y para gestion),
 * cada notificacion pertenece a un unico usuario y la ve cualquier rol.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notificacion_sistema")
public class NotificacionSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "tipo", nullable = false, length = 60)
    private String tipo;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "contenido", nullable = false, length = 1000)
    private String contenido;

    @Column(name = "leida", nullable = false)
    private boolean leida = false;

    @Column(name = "enviado_email", nullable = false)
    private boolean enviadoEmail = false;

    @Column(name = "entidad_relacionada", length = 60)
    private String entidadRelacionada;

    @Column(name = "entidad_relacionada_id")
    private Long entidadRelacionadaId;

    /** Ruta interna a la que lleva la notificacion al abrirla (null si no tiene destino). */
    @Column(name = "enlace", length = 300)
    private String enlace;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
