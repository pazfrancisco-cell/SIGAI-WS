package ar.edu.wellspring.sigaiws.modules.edfisica.domain;

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
 * Seccion 5.7.4: catalogo reutilizable de pruebas fisicas (nombre + unidad de
 * medida libre + direccion de mejora), compartido entre grupos y ciclos
 * lectivos. Reemplaza el tipo de prueba en texto libre para que el progreso
 * individual de un alumno pueda seguirse en una misma actividad a lo largo
 * del tiempo, sin depender de que el nombre se escriba identico cada vez.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "prueba_fisica_definicion")
public class PruebaFisicaDefinicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(name = "unidad", nullable = false, length = 40)
    private String unidad;

    @Column(name = "mayor_es_mejor", nullable = false)
    private boolean mayorEsMejor;

    @Column(name = "activa", nullable = false)
    private boolean activa = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por_id", nullable = false)
    private Usuario creadoPor;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
