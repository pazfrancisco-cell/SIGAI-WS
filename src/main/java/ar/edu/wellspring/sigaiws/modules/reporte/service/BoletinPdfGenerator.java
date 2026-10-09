package ar.edu.wellspring.sigaiws.modules.reporte.service;

import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
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

/** Seccion 5.8.1 (Boletin de Calificaciones), generado con OpenPDF (ver docs/DECISIONS.md ADR-002). */
@Component
public class BoletinPdfGenerator {

    /** Una fila por evaluacion cargada (seccion 5.3.3: tipo, fecha, titulo) con la nota obtenida. */
    public record FilaEvaluacion(String tipo, String fecha, String titulo, String valor) {}

    /** Una materia con el detalle completo de sus evaluaciones del cuatrimestre y el promedio final. */
    public record FilaMateria(String materia, List<FilaEvaluacion> evaluaciones, String promedio, String estado) {}

    public byte[] generar(Alumno alumno, CicloLectivo ciclo, int cuatrimestre, List<FilaMateria> filas) {
        Document documento = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(documento, out);
            documento.open();

            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font subtituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font materiaFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font textoFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font encabezadoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font promedioFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            Paragraph institucion = new Paragraph("Wellspring School", tituloFont);
            institucion.setAlignment(Element.ALIGN_CENTER);
            documento.add(institucion);

            Paragraph titulo = new Paragraph("Boletín de Calificaciones", subtituloFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(20);
            documento.add(titulo);

            documento.add(new Paragraph("Alumno: " + alumno.getNombreCompleto(), textoFont));
            documento.add(new Paragraph("Ciclo lectivo: " + ciclo.getAnio() + " - Cuatrimestre " + cuatrimestre, textoFont));
            documento.add(new Paragraph(" "));

            for (FilaMateria fila : filas) {
                Paragraph nombreMateria = new Paragraph(fila.materia(), materiaFont);
                nombreMateria.setSpacingBefore(10);
                nombreMateria.setSpacingAfter(6);
                documento.add(nombreMateria);

                if (fila.evaluaciones().isEmpty()) {
                    documento.add(new Paragraph("Sin evaluaciones cargadas en este cuatrimestre.", textoFont));
                } else {
                    PdfPTable tabla = new PdfPTable(4);
                    tabla.setWidthPercentage(100);
                    tabla.setWidths(new float[]{2f, 1.3f, 3f, 1.2f});
                    for (String encabezado : new String[]{"Tipo", "Fecha", "Título", "Nota"}) {
                        PdfPCell celda = new PdfPCell(new Phrase(encabezado, encabezadoFont));
                        celda.setGrayFill(0.85f);
                        tabla.addCell(celda);
                    }
                    for (FilaEvaluacion ev : fila.evaluaciones()) {
                        tabla.addCell(new PdfPCell(new Phrase(ev.tipo(), textoFont)));
                        tabla.addCell(new PdfPCell(new Phrase(ev.fecha(), textoFont)));
                        tabla.addCell(new PdfPCell(new Phrase(ev.titulo(), textoFont)));
                        tabla.addCell(new PdfPCell(new Phrase(ev.valor(), textoFont)));
                    }
                    documento.add(tabla);
                }

                Paragraph resumenMateria = new Paragraph(
                        "Promedio: " + fila.promedio() + "   —   Estado: " + fila.estado(), promedioFont);
                resumenMateria.setSpacingBefore(4);
                resumenMateria.setSpacingAfter(4);
                documento.add(resumenMateria);
            }

            documento.add(new Paragraph(" "));
            documento.add(new Paragraph("Firma del Director: ______________________", textoFont));

            documento.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("No se pudo generar el boletin: " + e.getMessage());
        }
    }
}
