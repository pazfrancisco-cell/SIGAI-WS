package ar.edu.wellspring.sigaiws.modules.configuracion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catalogo de workshops (seccion 5.6/5.14.2). La logica completa del modulo
 * (asignacion de alumnos, cambio obligatorio C1->C2) se construye en la Fase 4
 * del roadmap junto con Educacion Fisica; aqui solo vive el catalogo base,
 * necesario porque Alumno referencia workshop_c1_id/workshop_c2_id (seccion 5.1.5).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workshop")
public class Workshop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "banda", nullable = false, length = 20)
    private WorkshopBanda banda;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
