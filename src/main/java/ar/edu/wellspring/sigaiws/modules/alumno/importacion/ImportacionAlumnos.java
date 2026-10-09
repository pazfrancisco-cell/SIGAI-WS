package ar.edu.wellspring.sigaiws.modules.alumno.importacion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Importacion de alumnos en curso (seccion 5.14.2), guardada en la sesion del
 * usuario mientras revisa la vista previa: las filas validas se importan y las
 * que tienen errores quedan para corregirlas en pantalla e importarlas despues.
 */
@Getter
public class ImportacionAlumnos implements Serializable {

    public static final String ATRIBUTO_SESION = "importacionAlumnos";

    /** Columnas de la plantilla, en orden. */
    public static final List<String> COLUMNAS = List.of(
            "apellido", "nombre", "dni", "fecha_nacimiento", "genero", "nacionalidad", "domicilio", "localidad",
            "anio", "division",
            "tutor1_apellido", "tutor1_nombre", "tutor1_dni", "tutor1_vinculo", "tutor1_telefono", "tutor1_email",
            "tutor2_apellido", "tutor2_nombre", "tutor2_dni", "tutor2_vinculo", "tutor2_telefono", "tutor2_email");

    private final String nombreArchivo;
    private final List<Fila> filas = new ArrayList<>();

    public ImportacionAlumnos(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public List<Fila> pendientesValidas() {
        return filas.stream().filter(f -> !f.isImportada() && f.getErrores().isEmpty()).toList();
    }

    public List<Fila> conErrores() {
        return filas.stream().filter(f -> !f.isImportada() && !f.getErrores().isEmpty()).toList();
    }

    public List<Fila> importadas() {
        return filas.stream().filter(Fila::isImportada).toList();
    }

    public boolean terminada() {
        return filas.stream().allMatch(Fila::isImportada);
    }

    @Getter
    @Setter
    public static class Fila implements Serializable {
        /** Numero de fila en el archivo original (para que el usuario la ubique). */
        private final int numero;
        private final Map<String, String> valores = new LinkedHashMap<>();
        private final List<String> errores = new ArrayList<>();
        private boolean importada;
        private Long alumnoId;

        public Fila(int numero) {
            this.numero = numero;
        }

        public String valor(String columna) {
            String v = valores.get(columna);
            return v == null ? "" : v.trim();
        }

        public boolean tieneTutor(int n) {
            return COLUMNAS.stream().filter(c -> c.startsWith("tutor" + n + "_")).anyMatch(c -> !valor(c).isEmpty());
        }
    }
}
