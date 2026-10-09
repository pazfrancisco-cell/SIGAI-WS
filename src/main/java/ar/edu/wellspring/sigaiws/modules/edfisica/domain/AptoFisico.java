package ar.edu.wellspring.sigaiws.modules.edfisica.domain;

import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Seccion 5.7 (extension): apto fisico anual, uno por alumno y ciclo lectivo, vence el 1 de abril del año del ciclo. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "apto_fisico")
public class AptoFisico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciclo_lectivo_id", nullable = false)
    private CicloLectivo cicloLectivo;

    @Column(name = "fecha_presentacion", nullable = false)
    private LocalDate fechaPresentacion;

    @Column(name = "documento_path", length = 400)
    private String documentoPath;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cargado_por_id", nullable = false)
    private Usuario cargadoPor;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
