package ar.edu.wellspring.sigaiws.modules.usuario.domain;

import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Catalogo de roles institucionales (seccion 5.12.2), sembrado por V13__datos_iniciales.sql. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "nombre")
@Entity
@Table(name = "rol")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private RolNombre nombre;

    @Column(name = "etiqueta", nullable = false, length = 60)
    private String etiqueta;
}
