package ar.edu.wellspring.sigaiws.modules.alumno.domain;

import ar.edu.wellspring.sigaiws.common.crypto.EncryptedStringConverter;
import ar.edu.wellspring.sigaiws.common.domain.BaseEntity;
import ar.edu.wellspring.sigaiws.common.enums.Genero;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Modulo 5.1 (Gestion de Alumnos). Legajo digital institucional unico.
 * DNI, fecha de nacimiento y domicilio se persisten cifrados en la columna de
 * base de datos (seccion 7.2), pero el campo Java expone el valor ya
 * descifrado: {@link EncryptedStringConverter} descifra transparentemente al
 * leer y cifra al escribir, por lo que a nivel de objeto de dominio se trabaja
 * siempre con texto plano (los getters NO devuelven texto cifrado). El DNI
 * ademas mantiene un hash SHA-256 (dniHash) para validar unicidad y buscar sin
 * necesidad de descifrar todo el listado.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "alumno")
public class Alumno extends BaseEntity {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    @Column(name = "apellido", nullable = false, length = 80)
    private String apellido;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "dni_cifrado", nullable = false, length = 256)
    private String dni;

    @Column(name = "dni_hash", nullable = false, unique = true, length = 64)
    private String dniHash;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "fecha_nacimiento_cifrada", nullable = false, length = 128)
    private String fechaNacimientoIso;

    @Enumerated(EnumType.STRING)
    @Column(name = "genero", length = 20)
    private Genero genero;

    @Column(name = "nacionalidad", nullable = false, length = 80)
    private String nacionalidad = "Argentina";

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "domicilio_cifrado", nullable = false, length = 512)
    private String domicilio;

    @Column(name = "localidad", nullable = false, length = 120)
    private String localidad;

    @Column(name = "foto_path", length = 300)
    private String fotoPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_global", nullable = false, length = 20)
    private EstadoAlumno estadoGlobal = EstadoAlumno.ACTIVO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciclo_lectivo_ingreso_id", nullable = false)
    private CicloLectivo cicloLectivoIngreso;

    @Transient
    public LocalDate getFechaNacimiento() {
        return fechaNacimientoIso != null ? LocalDate.parse(fechaNacimientoIso, ISO) : null;
    }

    public void setFechaNacimiento(LocalDate fecha) {
        this.fechaNacimientoIso = fecha != null ? fecha.format(ISO) : null;
    }

    @Transient
    public String getNombreCompleto() {
        return apellido + ", " + nombre;
    }
}
