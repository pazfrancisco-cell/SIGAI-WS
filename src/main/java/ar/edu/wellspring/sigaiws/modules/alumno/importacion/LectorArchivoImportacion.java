package ar.edu.wellspring.sigaiws.modules.alumno.importacion;

import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lee la planilla de importacion de alumnos: Excel (.xlsx/.xls) o CSV. Las
 * columnas se reconocen por el encabezado (sin importar mayusculas, acentos u
 * orden); las que no se reconocen se ignoran.
 */
@Component
public class LectorArchivoImportacion {

    private static final int MAX_FILAS = 2000;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ImportacionAlumnos leer(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("Elegi un archivo para importar.");
        }
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        List<List<String>> tabla;
        try {
            byte[] contenido = archivo.getBytes();
            if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls")) {
                tabla = leerExcel(contenido);
            } else if (nombre.endsWith(".csv") || nombre.endsWith(".txt")) {
                tabla = leerCsv(contenido);
            } else {
                throw new BusinessException("Formato no soportado: subi un archivo .xlsx, .xls o .csv.");
            }
        } catch (IOException e) {
            throw new BusinessException("No se pudo leer el archivo: " + e.getMessage());
        }
        return armar(archivo.getOriginalFilename(), tabla);
    }

    private ImportacionAlumnos armar(String nombreArchivo, List<List<String>> tabla) {
        if (tabla.isEmpty()) {
            throw new BusinessException("El archivo esta vacio.");
        }
        List<String> encabezados = tabla.get(0).stream().map(LectorArchivoImportacion::normalizarEncabezado).toList();
        List<String> faltantes = List.of("apellido", "nombre", "dni").stream().filter(c -> !encabezados.contains(c)).toList();
        if (!faltantes.isEmpty()) {
            throw new BusinessException("La primera fila tiene que ser la de encabezados de la plantilla. No se encontraron: "
                    + String.join(", ", faltantes) + ". Descarga la plantilla para ver el formato.");
        }

        ImportacionAlumnos importacion = new ImportacionAlumnos(nombreArchivo);
        for (int i = 1; i < tabla.size(); i++) {
            List<String> celdas = tabla.get(i);
            if (celdas.stream().allMatch(c -> c == null || c.isBlank())) {
                continue;
            }
            if (importacion.getFilas().size() >= MAX_FILAS) {
                throw new BusinessException("El archivo supera el maximo de " + MAX_FILAS + " alumnos por importacion. Dividilo en partes.");
            }
            ImportacionAlumnos.Fila fila = new ImportacionAlumnos.Fila(i + 1);
            for (int c = 0; c < encabezados.size(); c++) {
                String columna = encabezados.get(c);
                if (ImportacionAlumnos.COLUMNAS.contains(columna)) {
                    fila.getValores().put(columna, c < celdas.size() && celdas.get(c) != null ? celdas.get(c).trim() : "");
                }
            }
            importacion.getFilas().add(fila);
        }
        if (importacion.getFilas().isEmpty()) {
            throw new BusinessException("El archivo no tiene alumnos debajo de los encabezados.");
        }
        return importacion;
    }

    private List<List<String>> leerExcel(byte[] contenido) throws IOException {
        DataFormatter formato = new DataFormatter(Locale.forLanguageTag("es-AR"));
        List<List<String>> tabla = new ArrayList<>();
        try (Workbook libro = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
            Sheet hoja = libro.getSheetAt(0);
            for (Row fila : hoja) {
                List<String> celdas = new ArrayList<>();
                for (int c = 0; c < fila.getLastCellNum(); c++) {
                    Cell celda = fila.getCell(c);
                    celdas.add(celda == null ? "" : textoDeCelda(celda, formato));
                }
                tabla.add(celdas);
            }
        }
        return tabla;
    }

    private String textoDeCelda(Cell celda, DataFormatter formato) {
        if (celda.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(celda)) {
            return celda.getLocalDateTimeCellValue().toLocalDate().format(FECHA);
        }
        return formato.formatCellValue(celda);
    }

    private List<List<String>> leerCsv(byte[] contenido) {
        String texto = decodificar(contenido);
        String[] lineas = texto.split("\\r?\\n", -1);
        char separador = lineas[0].chars().filter(ch -> ch == ';').count() >= lineas[0].chars().filter(ch -> ch == ',').count() ? ';' : ',';
        List<List<String>> tabla = new ArrayList<>();
        for (String linea : lineas) {
            if (!linea.isBlank()) {
                tabla.add(separarCsv(linea, separador));
            }
        }
        return tabla;
    }

    /** UTF-8 (con o sin BOM) o, si no lo es, Windows-1252: lo que guarda Excel en español como "CSV". */
    private static String decodificar(byte[] contenido) {
        if (contenido.length >= 3 && (contenido[0] & 0xFF) == 0xEF && (contenido[1] & 0xFF) == 0xBB && (contenido[2] & 0xFF) == 0xBF) {
            return new String(contenido, 3, contenido.length - 3, StandardCharsets.UTF_8);
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(contenido)).toString();
        } catch (CharacterCodingException e) {
            return new String(contenido, Charset.forName("windows-1252"));
        }
    }

    private static List<String> separarCsv(String linea, char separador) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char ch = linea.charAt(i);
            if (ch == '"') {
                if (entreComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    entreComillas = !entreComillas;
                }
            } else if (ch == separador && !entreComillas) {
                campos.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(ch);
            }
        }
        campos.add(actual.toString());
        return campos;
    }

    /** "Fecha de Nacimiento" -> "fecha_de_nacimiento"; "Año" -> "anio"; "División" -> "division". */
    static String normalizarEncabezado(String encabezado) {
        String limpio = Normalizer.normalize(encabezado == null ? "" : encabezado.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
        return switch (limpio) {
            case "ano" -> "anio";
            case "fecha_de_nacimiento" -> "fecha_nacimiento";
            default -> limpio;
        };
    }
}
