package ar.edu.wellspring.sigaiws.modules.mensajeria.domain;

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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Seccion 5.10.2 (Mensajeria Directa y grupal). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "conversacion")
public class Conversacion {

    public enum Tipo { DIRECTA, GRUPAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private Tipo tipo = Tipo.DIRECTA;

    /** Solo grupos. */
    @Column(name = "nombre", length = 120)
    private String nombre;

    /** Solo grupos: unico autorizado a agregar o quitar integrantes. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id")
    private Usuario creadoPor;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();

    public boolean isGrupal() {
        return tipo == Tipo.GRUPAL;
    }
}
