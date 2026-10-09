package ar.edu.wellspring.sigaiws.modules.workshop.domain;

import ar.edu.wellspring.sigaiws.modules.configuracion.domain.Workshop;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import jakarta.persistence.Entity;
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

/** Seccion 5.6.2: un docente esta asignado a un unico workshop. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workshop_docente")
public class WorkshopDocente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workshop_id", nullable = false)
    private Workshop workshop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_docente_id", nullable = false, unique = true)
    private Usuario docente;
}
