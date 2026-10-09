package ar.edu.wellspring.sigaiws.modules.alumno.dto;

import ar.edu.wellspring.sigaiws.common.enums.Genero;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.EstadoAlumno;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Seccion 5.1.5: formulario Nuevo Alumno / Editar Alumno, organizado en las
 * 4 secciones del documento (Datos Personales, Datos Academicos, Tutores,
 * Foto y Documentacion - esta ultima se maneja aparte via multipart).
 */
@Getter
@Setter
public class AlumnoForm {

    private Long id;

    // --- Seccion 1: Datos Personales ---
    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 60)
    private String apellido;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60)
    private String nombre;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{7,8}", message = "El DNI debe tener 7 u 8 digitos")
    private String dni;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaNacimiento;

    private Genero genero;

    @NotBlank(message = "La nacionalidad es obligatoria")
    private String nacionalidad = "Argentina";

    @NotBlank(message = "El domicilio es obligatorio")
    private String domicilio;

    @NotBlank(message = "La localidad es obligatoria")
    private String localidad;

    // --- Seccion 2: Datos Academicos ---
    @NotNull(message = "Seleccione el año/curso")
    @Min(1) @Max(6)
    private Integer anio;

    @NotBlank(message = "Seleccione la division")
    private String division;

    @NotNull(message = "Seleccione el estado")
    private EstadoAlumno estado = EstadoAlumno.ACTIVO;

    @NotNull(message = "Seleccione el ciclo lectivo de ingreso")
    private Long cicloLectivoIngresoId;

    private boolean repetidor = false;

    /** Año/division que repite; obligatorio solo si repetidor=true (validado en service). */
    private Long cursoDivisionAnteriorId;

    // --- Seccion 3: Tutores (hasta 2) ---
    @Valid
    @Size(max = 2, message = "Se permiten como maximo 2 tutores por alumno")
    private List<TutorForm> tutores = new ArrayList<>();
}
