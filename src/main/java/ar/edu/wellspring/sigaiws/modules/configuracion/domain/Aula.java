package ar.edu.wellspring.sigaiws.modules.configuracion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Seccion 5.5.1 ("Administracion de aulas"), sin pantalla propia detallada en el documento -> CRUD minimo inferido. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "aula")
public class Aula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 60)
    private String nombre;

    @Column(name = "capacidad")
    private Integer capacidad;

    @Column(name = "activa", nullable = false)
    private boolean activa = true;
}
