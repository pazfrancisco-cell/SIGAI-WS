package ar.edu.wellspring.sigaiws.modules.configuracion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Seccion 5.14.2. Solo puede haber un ciclo lectivo activo a la vez (garantizado
 * ademas por el indice unico filtrado ux_ciclo_lectivo_activo en la base de datos).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ciclo_lectivo")
public class CicloLectivo {

    public enum Estado { PLANIFICADO, ACTIVO, CERRADO, ARCHIVADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anio", nullable = false, unique = true)
    private Integer anio;

    @Column(name = "fecha_inicio_c1", nullable = false)
    private LocalDate fechaInicioC1;

    @Column(name = "fecha_fin_c1", nullable = false)
    private LocalDate fechaFinC1;

    @Column(name = "fecha_inicio_c2", nullable = false)
    private LocalDate fechaInicioC2;

    @Column(name = "fecha_fin_c2", nullable = false)
    private LocalDate fechaFinC2;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado = Estado.PLANIFICADO;

    @Column(name = "activo", nullable = false)
    private boolean activo = false;

    @Column(name = "cuatrimestre_activo")
    private Integer cuatrimestreActivo;

    /** Seccion 6.4 (Flujo: Cierre de Cuatrimestre): bloquea la carga directa de notas del cuatrimestre. */
    @Column(name = "cuatrimestre1_cerrado", nullable = false)
    private boolean cuatrimestre1Cerrado = false;

    @Column(name = "cuatrimestre2_cerrado", nullable = false)
    private boolean cuatrimestre2Cerrado = false;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    /**
     * Cuatrimestre al que corresponde la fecha de hoy segun fechaInicioC1/FinC1/
     * InicioC2/FinC2: es el valor por defecto para cargas "en el momento"
     * (calificaciones, pruebas fisicas, etc.), independiente del campo
     * cuatrimestreActivo (que un Superadmin puede setear manualmente y que hoy
     * no bloquea ni habilita nada por si solo). Antes del inicio de C1 o durante
     * C1 devuelve 1; desde el fin de C1 en adelante (incluido el receso entre
     * cuatrimestres y todo C2) devuelve 2.
     */
    public int getCuatrimestreEnCurso() {
        return LocalDate.now().isAfter(fechaFinC1) ? 2 : 1;
    }
}
