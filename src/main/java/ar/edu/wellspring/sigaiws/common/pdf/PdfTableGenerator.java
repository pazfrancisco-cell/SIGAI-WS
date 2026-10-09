package ar.edu.wellspring.sigaiws.common.pdf;

import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Generador de PDF tabular generico (OpenPDF, ver ADR-002), reutilizado por
 * los reportes institucionales de la seccion 5.8.3 y por otras exportaciones
 * (ej. horario docente) para no duplicar el boilerplate de Document/PdfWriter
 * en cada generador especifico.
 */
@Component
public class PdfTableGenerator {

    public byte[] generar(String titulo, List<String> lineasEncabezado, List<String> columnas, List<List<String>> filas) {
        Document documento = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(documento, out);
            documento.open();

            Font tituloInstitucionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font textoFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font encabezadoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

            Paragraph institucion = new Paragraph("Wellspring School", tituloInstitucionFont);
            institucion.setAlignment(Element.ALIGN_CENTER);
            documento.add(institucion);

            Paragraph tituloParrafo = new Paragraph(titulo, tituloFont);
            tituloParrafo.setAlignment(Element.ALIGN_CENTER);
            tituloParrafo.setSpacingAfter(10);
            documento.add(tituloParrafo);

            for (String linea : lineasEncabezado) {
                documento.add(new Paragraph(linea, textoFont));
            }
            documento.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(columnas.size());
            tabla.setWidthPercentage(100);
            for (String encabezado : columnas) {
                PdfPCell celda = new PdfPCell(new Phrase(encabezado, encabezadoFont));
                celda.setGrayFill(0.85f);
                tabla.addCell(celda);
            }
            for (List<String> fila : filas) {
                for (String valor : fila) {
                    tabla.addCell(new PdfPCell(new Phrase(valor != null ? valor : "-", textoFont)));
                }
            }
            documento.add(tabla);

            documento.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("No se pudo generar el PDF: " + e.getMessage());
        }
    }

    /** Una seccion con su propio subtitulo y tabla, para reportes organizados en bloques (ej. por mes, por ciclo lectivo). */
    public record Seccion(String subtitulo, List<String> columnas, List<List<String>> filas) {}

    /** Variante de {@link #generar} que organiza los datos en secciones con subtitulo propio, cada una con su tabla. */
    public byte[] generarAgrupado(String titulo, List<String> lineasEncabezado, List<Seccion> secciones) {
        Document documento = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(documento, out);
            documento.open();

            Font tituloInstitucionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font subtituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font textoFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font encabezadoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

            Paragraph institucion = new Paragraph("Wellspring School", tituloInstitucionFont);
            institucion.setAlignment(Element.ALIGN_CENTER);
            documento.add(institucion);

            Paragraph tituloParrafo = new Paragraph(titulo, tituloFont);
            tituloParrafo.setAlignment(Element.ALIGN_CENTER);
            tituloParrafo.setSpacingAfter(10);
            documento.add(tituloParrafo);

            for (String linea : lineasEncabezado) {
                documento.add(new Paragraph(linea, textoFont));
            }
            documento.add(new Paragraph(" "));

            for (Seccion seccion : secciones) {
                Paragraph subtitulo = new Paragraph(seccion.subtitulo(), subtituloFont);
                subtitulo.setSpacingBefore(8);
                subtitulo.setSpacingAfter(6);
                documento.add(subtitulo);

                if (seccion.filas().isEmpty()) {
                    documento.add(new Paragraph("Sin registros.", textoFont));
                    continue;
                }
                PdfPTable tabla = new PdfPTable(seccion.columnas().size());
                tabla.setWidthPercentage(100);
                for (String encabezado : seccion.columnas()) {
                    PdfPCell celda = new PdfPCell(new Phrase(encabezado, encabezadoFont));
                    celda.setGrayFill(0.85f);
                    tabla.addCell(celda);
                }
                for (List<String> fila : seccion.filas()) {
                    for (String valor : fila) {
                        tabla.addCell(new PdfPCell(new Phrase(valor != null ? valor : "-", textoFont)));
                    }
                }
                documento.add(tabla);
            }

            documento.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("No se pudo generar el PDF: " + e.getMessage());
        }
    }
}
