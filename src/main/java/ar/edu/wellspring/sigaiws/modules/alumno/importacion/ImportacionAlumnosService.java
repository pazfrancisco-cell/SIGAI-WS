package ar.edu.wellspring.sigaiws.modules.alumno.importacion;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.enums.Genero;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.util.HashUtil;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.EstadoAlumno;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Tutor;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.VinculoTutor;
import ar.edu.wellspring.sigaiws.modules.alumno.dto.AlumnoForm;
import ar.edu.wellspring.sigaiws.modules.alumno.dto.TutorForm;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.TutorRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.service.AlumnoService;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seccion 5.14.2: importacion masiva de alumnos y sus tutores desde Excel/CSV.
 * Cada alumno se crea con el mismo alta que el formulario (AlumnoService.crear),
 * asi que los tutores nuevos reciben su cuenta del Portal de Padres. Las filas
 * validas se importan y las que tienen errores quedan para corregir en pantalla.
 */
@Service
@RequiredArgsConstructor
public class ImportacionAlumnosService {

    private static final String MODULO = "ALUMNOS";
    private static final Pattern DNI = Pattern.compile("\\d{7,8}");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
            DateTimeFormatter.ofPattern("d/M/yyyy"), DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"), DateTimeFormatter.ofPattern("d/M/yy"));

    private final AlumnoService alumnoService;
    private final AlumnoRepository alumnoRepository;
    private final TutorRepository tutorRepository;
    private final UsuarioRepository usuarioRepository;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final CursoDivisionRepository cursoDivisionRepository;
    private final AuditService auditService;

    public record Resultado(int importados, int cuentasPortalCreadas, int fallaron) {}

    // ---------------------------------------------------------------- plantilla

    public byte[] plantilla() {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Font negrita = libro.createFont();
            negrita.setBold(true);
            CellStyle estiloEncabezado = libro.createCellStyle();
            estiloEncabezado.setFont(negrita);
            CellStyle estiloTexto = libro.createCellStyle();
            estiloTexto.setDataFormat(libro.createDataFormat().getFormat("@"));

            Sheet hoja = libro.createSheet("Alumnos");
            Row encabezado = hoja.createRow(0);
            Row ejemplo = hoja.createRow(1);
            List<String> valoresEjemplo = List.of(
                    "Gomez", "Lucia", "45123456", "15/03/2012", "F", "Argentina", "Av. Siempre Viva 742", "Pilar",
                    "1", "A",
                    "Gomez", "Carlos", "25123456", "PADRE", "1155550000", "carlos.gomez@example.com",
                    "", "", "", "", "", "");
            for (int c = 0; c < ImportacionAlumnos.COLUMNAS.size(); c++) {
                var celda = encabezado.createCell(c);
                celda.setCellValue(ImportacionAlumnos.COLUMNAS.get(c));
                celda.setCellStyle(estiloEncabezado);
                var celdaEjemplo = ejemplo.createCell(c);
                celdaEjemplo.setCellValue(valoresEjemplo.get(c));
                celdaEjemplo.setCellStyle(estiloTexto);
                hoja.setDefaultColumnStyle(c, estiloTexto);
                hoja.setColumnWidth(c, 18 * 256);
            }
            hoja.createFreezePane(0, 1);

            Sheet instrucciones = libro.createSheet("Instrucciones");
            String[] lineas = {
                    "Como completar la planilla",
                    "",
                    "- Una fila por alumno, a partir de la fila 2 de la hoja 'Alumnos'. Borra la fila de ejemplo.",
                    "- No cambies los encabezados de la fila 1 (el orden de las columnas puede variar).",
                    "- Obligatorios: apellido, nombre, dni, fecha_nacimiento, domicilio, localidad, anio, division y los datos del tutor 1.",
                    "- dni: 7 u 8 digitos, sin puntos.",
                    "- fecha_nacimiento: dd/mm/aaaa.",
                    "- genero (opcional): M, F, OTRO o PREFIERO_NO_DECIR.",
                    "- nacionalidad: si se deja vacia se usa Argentina.",
                    "- anio: 1 a 6. division: la letra del curso (A, B...). El curso tiene que existir y estar activo.",
                    "- tutorN_vinculo: PADRE, MADRE, TUTOR_LEGAL u OTRO.",
                    "- El tutor 2 es opcional: si se completa, van todos sus datos.",
                    "- Si un tutor ya existe en el sistema (mismo DNI) se vincula al alumno y se actualizan su telefono y email.",
                    "- Los tutores nuevos reciben por email su cuenta del Portal de Padres.",
                    "- Los alumnos se cargan en el ciclo lectivo activo, en estado Activo.",
                    "- Tambien se puede subir como CSV (separado por ; o ,) con los mismos encabezados.",
            };
            for (int i = 0; i < lineas.length; i++) {
                instrucciones.createRow(i).createCell(0).setCellValue(lineas[i]);
            }
            instrucciones.setColumnWidth(0, 110 * 256);

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("No se pudo generar la plantilla: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- validacion

    /** Reemplaza los valores de las filas pendientes con lo que el usuario corrigio en pantalla (parametros "f.{numero}.{columna}"). */
    public void aplicarCorrecciones(ImportacionAlumnos importacion, Map<String, String> parametros) {
        for (ImportacionAlumnos.Fila fila : importacion.getFilas()) {
            if (fila.isImportada()) {
                continue;
            }
            for (String columna : ImportacionAlumnos.COLUMNAS) {
                String valor = parametros.get("f." + fila.getNumero() + "." + columna);
                if (valor != null) {
                    fila.getValores().put(columna, valor.trim());
                }
            }
        }
    }

    /** Vuelve a validar todas las filas pendientes contra la base y entre si. */
    @Transactional(readOnly = true)
    public void validar(ImportacionAlumnos importacion) {
        Set<String> dnisAlumnoEnArchivo = new HashSet<>();
        Map<String, String> emailPorDniTutorNuevo = new HashMap<>();
        Map<String, String> dniPorEmailTutorNuevo = new HashMap<>();
        for (ImportacionAlumnos.Fila fila : importacion.getFilas()) {
            if (fila.isImportada()) {
                continue;
            }
            fila.getErrores().clear();
            List<String> e = fila.getErrores();

            requerido(fila, "apellido", "Falta el apellido.", e);
            requerido(fila, "nombre", "Falta el nombre.", e);
            String dni = limpiarDni(fila, "dni");
            if (dni.isEmpty()) {
                e.add("Falta el DNI.");
            } else if (!DNI.matcher(dni).matches()) {
                e.add("El DNI debe tener 7 u 8 digitos.");
            } else if (!dnisAlumnoEnArchivo.add(dni)) {
                e.add("El DNI " + dni + " esta repetido en el archivo.");
            } else if (alumnoRepository.existsByDniHash(HashUtil.sha256(dni))) {
                e.add("Ya existe un alumno con DNI " + dni + ".");
            }

            String fechaTexto = fila.valor("fecha_nacimiento");
            if (fechaTexto.isEmpty()) {
                e.add("Falta la fecha de nacimiento.");
            } else {
                LocalDate fecha = parsearFecha(fechaTexto);
                if (fecha == null) {
                    e.add("Fecha de nacimiento invalida (usa dd/mm/aaaa).");
                } else {
                    try {
                        alumnoService.validarFechaNacimiento(fecha);
                    } catch (BusinessException ex) {
                        e.add(ex.getMessage());
                    }
                }
            }
            if (!fila.valor("genero").isEmpty() && parsearGenero(fila.valor("genero")) == null) {
                e.add("Genero invalido (M, F, OTRO o PREFIERO_NO_DECIR).");
            }
            requerido(fila, "domicilio", "Falta el domicilio.", e);
            requerido(fila, "localidad", "Falta la localidad.", e);
            validarCurso(fila, e);

            if (!fila.tieneTutor(1)) {
                e.add("Falta el tutor 1: cada alumno necesita al menos un tutor.");
            } else {
                validarTutor(fila, 1, e, emailPorDniTutorNuevo, dniPorEmailTutorNuevo);
            }
            if (fila.tieneTutor(2)) {
                validarTutor(fila, 2, e, emailPorDniTutorNuevo, dniPorEmailTutorNuevo);
                if (!limpiarDni(fila, "tutor1_dni").isEmpty() && limpiarDni(fila, "tutor1_dni").equals(limpiarDni(fila, "tutor2_dni"))) {
                    e.add("El tutor 1 y el tutor 2 tienen el mismo DNI.");
                }
            }
        }
    }

    private void validarCurso(ImportacionAlumnos.Fila fila, List<String> e) {
        String anioTexto = fila.valor("anio").replaceAll("[^0-9]", "");
        String division = fila.valor("division").toUpperCase(Locale.ROOT);
        if (anioTexto.isEmpty() || division.isEmpty()) {
            e.add("Falta el año o la division.");
            return;
        }
        int anio = Integer.parseInt(anioTexto);
        var curso = cursoDivisionRepository.findByAnioAndDivision(anio, division);
        if (curso.isEmpty()) {
            e.add("No existe el curso " + anio + "° " + division + ".");
        } else if (!curso.get().isActiva()) {
            e.add("El curso " + anio + "° " + division + " esta desactivado.");
        }
    }

    private void validarTutor(ImportacionAlumnos.Fila fila, int n, List<String> e,
                              Map<String, String> emailPorDniTutorNuevo, Map<String, String> dniPorEmailTutorNuevo) {
        String p = "tutor" + n + "_";
        String t = "Tutor " + n + ": ";
        requerido(fila, p + "apellido", t + "falta el apellido.", e);
        requerido(fila, p + "nombre", t + "falta el nombre.", e);
        requerido(fila, p + "telefono", t + "falta el telefono.", e);
        if (fila.valor(p + "vinculo").isEmpty()) {
            e.add(t + "falta el vinculo.");
        } else if (parsearVinculo(fila.valor(p + "vinculo")) == null) {
            e.add(t + "vinculo invalido (PADRE, MADRE, TUTOR_LEGAL u OTRO).");
        }
        String email = fila.valor(p + "email").toLowerCase(Locale.ROOT);
        boolean emailValido = !email.isEmpty() && EMAIL.matcher(email).matches();
        if (email.isEmpty()) {
            e.add(t + "falta el email.");
        } else if (!emailValido) {
            e.add(t + "el email no tiene un formato valido.");
        }

        String dni = limpiarDni(fila, p + "dni");
        if (dni.isEmpty()) {
            e.add(t + "falta el DNI.");
            return;
        }
        if (!DNI.matcher(dni).matches()) {
            e.add(t + "el DNI debe tener 7 u 8 digitos.");
            return;
        }
        if (!emailValido) {
            return;
        }
        Optional<Tutor> existente = tutorRepository.findByDniHash(HashUtil.sha256(dni));
        if (existente.isPresent()) {
            if (!existente.get().getEmail().equalsIgnoreCase(email) && tutorRepository.existsByEmailIgnoreCase(email)) {
                e.add(t + "el email " + email + " ya lo usa otro tutor.");
            }
            return;
        }
        // Tutor nuevo: se le va a crear una cuenta del portal con ese email.
        String emailPrevio = emailPorDniTutorNuevo.putIfAbsent(dni, email);
        String dniPrevio = dniPorEmailTutorNuevo.putIfAbsent(email, dni);
        if (emailPrevio != null && !emailPrevio.equals(email)) {
            e.add(t + "el DNI " + dni + " aparece en otra fila con otro email (" + emailPrevio + ").");
        } else if (dniPrevio != null && !dniPrevio.equals(dni)) {
            e.add(t + "el email " + email + " aparece en otra fila con otro DNI de tutor.");
        } else if (tutorRepository.existsByEmailIgnoreCase(email) || usuarioRepository.existsByEmailIgnoreCase(email)) {
            e.add(t + "el email " + email + " ya esta registrado en el sistema.");
        }
    }

    // ---------------------------------------------------------------- importacion

    /** Importa las filas validas, cada una en su propia transaccion: si una falla, queda con el error y las demas siguen. */
    public Resultado importarValidas(ImportacionAlumnos importacion) {
        CicloLectivo cicloActivo = cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo: activalo antes de importar alumnos."));
        int importados = 0;
        int cuentasCreadas = 0;
        int fallaron = 0;
        for (ImportacionAlumnos.Fila fila : importacion.pendientesValidas()) {
            List<String> dnisTutores = new ArrayList<>();
            for (int n = 1; n <= 2; n++) {
                if (fila.tieneTutor(n)) {
                    dnisTutores.add(limpiarDni(fila, "tutor" + n + "_dni"));
                }
            }
            long tutoresNuevos = dnisTutores.stream()
                    .filter(d -> tutorRepository.findByDniHash(HashUtil.sha256(d)).isEmpty()).count();
            try {
                var alumno = alumnoService.crear(armarFormulario(fila, cicloActivo.getId()));
                fila.setImportada(true);
                fila.setAlumnoId(alumno.getId());
                importados++;
                cuentasCreadas += (int) tutoresNuevos;
            } catch (RuntimeException ex) {
                fila.getErrores().add(ex.getMessage() == null ? "No se pudo importar la fila." : ex.getMessage());
                fallaron++;
            }
        }
        if (importados > 0) {
            auditService.registrar(MODULO, "IMPORTAR", "Alumno", null, null,
                    "archivo=" + importacion.getNombreArchivo() + ", alumnos=" + importados + ", cuentasPortal=" + cuentasCreadas);
        }
        return new Resultado(importados, cuentasCreadas, fallaron);
    }

    private AlumnoForm armarFormulario(ImportacionAlumnos.Fila fila, Long cicloActivoId) {
        AlumnoForm form = new AlumnoForm();
        form.setApellido(fila.valor("apellido"));
        form.setNombre(fila.valor("nombre"));
        form.setDni(limpiarDni(fila, "dni"));
        form.setFechaNacimiento(parsearFecha(fila.valor("fecha_nacimiento")));
        form.setGenero(fila.valor("genero").isEmpty() ? null : parsearGenero(fila.valor("genero")));
        form.setNacionalidad(fila.valor("nacionalidad").isEmpty() ? "Argentina" : fila.valor("nacionalidad"));
        form.setDomicilio(fila.valor("domicilio"));
        form.setLocalidad(fila.valor("localidad"));
        form.setAnio(Integer.parseInt(fila.valor("anio").replaceAll("[^0-9]", "")));
        form.setDivision(fila.valor("division").toUpperCase(Locale.ROOT));
        form.setEstado(EstadoAlumno.ACTIVO);
        form.setCicloLectivoIngresoId(cicloActivoId);
        for (int n = 1; n <= 2; n++) {
            if (!fila.tieneTutor(n)) {
                continue;
            }
            String p = "tutor" + n + "_";
            TutorForm tutor = new TutorForm();
            tutor.setApellido(fila.valor(p + "apellido"));
            tutor.setNombre(fila.valor(p + "nombre"));
            tutor.setDni(limpiarDni(fila, p + "dni"));
            tutor.setVinculo(parsearVinculo(fila.valor(p + "vinculo")));
            tutor.setTelefono(fila.valor(p + "telefono"));
            tutor.setEmail(fila.valor(p + "email").toLowerCase(Locale.ROOT));
            tutor.setRecibeNotificaciones(true);
            form.getTutores().add(tutor);
        }
        return form;
    }

    // ---------------------------------------------------------------- parseo

    private static void requerido(ImportacionAlumnos.Fila fila, String columna, String mensaje, List<String> errores) {
        if (fila.valor(columna).isEmpty()) {
            errores.add(mensaje);
        }
    }

    /** "45.123.456" o "45 123 456" -> "45123456". */
    private static String limpiarDni(ImportacionAlumnos.Fila fila, String columna) {
        return fila.valor(columna).replaceAll("[.\\s-]", "");
    }

    static LocalDate parsearFecha(String texto) {
        for (DateTimeFormatter formato : FORMATOS_FECHA) {
            try {
                LocalDate fecha = LocalDate.parse(texto.trim(), formato);
                // "d/M/yy" interpreta 12 como 2012; un año de nacimiento en el futuro es del siglo pasado.
                return fecha.isAfter(LocalDate.now()) && formato == FORMATOS_FECHA.get(3) ? fecha.minusYears(100) : fecha;
            } catch (DateTimeParseException ignorado) {
                // probar el siguiente formato
            }
        }
        return null;
    }

    static Genero parsearGenero(String texto) {
        return switch (normalizar(texto)) {
            case "M", "MASCULINO", "VARON", "H", "HOMBRE" -> Genero.MASCULINO;
            case "F", "FEMENINO", "MUJER" -> Genero.FEMENINO;
            case "OTRO", "X" -> Genero.OTRO;
            case "PREFIERO_NO_DECIR", "NO_INFORMA" -> Genero.PREFIERO_NO_DECIR;
            default -> null;
        };
    }

    static VinculoTutor parsearVinculo(String texto) {
        return switch (normalizar(texto)) {
            case "PADRE" -> VinculoTutor.PADRE;
            case "MADRE" -> VinculoTutor.MADRE;
            case "TUTOR_LEGAL", "TUTOR", "TUTORA", "TUTORA_LEGAL" -> VinculoTutor.TUTOR_LEGAL;
            case "OTRO", "OTRA" -> VinculoTutor.OTRO;
            default -> null;
        };
    }

    /** "Tutor legal" -> "TUTOR_LEGAL"; "Varón" -> "VARON". */
    private static String normalizar(String texto) {
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z]+", "_")
                .replaceAll("^_|_$", "");
    }
}
