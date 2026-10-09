package ar.edu.wellspring.sigaiws.common.pdf;

import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Exportacion a Excel (.xlsx) de los mismos reportes tabulares que ya se
 * generan en PDF (seccion 5.8.3), via Apache POI. Sin estilos elaborados: solo
 * encabezado en negrita y columnas auto-ajustadas, para que el dato sea facil
 * de reusar (filtrar, ordenar) en una planilla.
 */
@Component
public class ExcelTableGenerator {

    public byte[] generar(String titulo, List<String> columnas, List<List<String>> filas) {
        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            XSSFSheet hoja = libro.createSheet(recortarNombreHoja(titulo));

            CellStyle estiloEncabezado = libro.createCellStyle();
            Font fuenteNegrita = libro.createFont();
            fuenteNegrita.setBold(true);
            estiloEncabezado.setFont(fuenteNegrita);

            Row filaEncabezado = hoja.createRow(0);
            for (int i = 0; i < columnas.size(); i++) {
                Cell celda = filaEncabezado.createCell(i);
                celda.setCellValue(columnas.get(i));
                celda.setCellStyle(estiloEncabezado);
            }

            int numeroFila = 1;
            for (List<String> fila : filas) {
                Row filaExcel = hoja.createRow(numeroFila++);
                for (int i = 0; i < fila.size(); i++) {
                    filaExcel.createCell(i).setCellValue(fila.get(i) != null ? fila.get(i) : "-");
                }
            }

            for (int i = 0; i < columnas.size(); i++) {
                hoja.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            libro.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("No se pudo generar el Excel: " + e.getMessage());
        }
    }

    private String recortarNombreHoja(String titulo) {
        String limpio = titulo.replaceAll("[\\\\/?*\\[\\]:]", " ");
        return limpio.length() > 31 ? limpio.substring(0, 31) : limpio;
    }
}
