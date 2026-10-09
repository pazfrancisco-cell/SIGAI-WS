package ar.edu.wellspring.sigaiws.modules.alumno.service;

import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoCicloLectivo;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.PaseAlumno;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Genera el "documento de pase en PDF con el historial academico del ciclo
 * lectivo vigente" (seccion 5.1.8) usando OpenPDF (ver docs/DECISIONS.md ADR-002).
 * El historial academico (calificaciones) se incorporara cuando el modulo
 * Calificaciones (Fase 3 del roadmap) este implementado; por ahora el documento
 * refleja la trayectoria institucional (años cursados, division, condicion).
 */
@Component
public class PasePdfGenerator {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generar(Alumno alumno, PaseAlumno pase, List<AlumnoCicloLectivo> trayectoria) {
        Document documento = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(documento, out);
            documento.open();

            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font subtituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font textoFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph institucion = new Paragraph("Wellspring School", tituloFont);
            institucion.setAlignment(Element.ALIGN_CENTER);
            documento.add(institucion);

            Paragraph titulo = new Paragraph("Documento de Pase", subtituloFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(20);
            documento.add(titulo);

            documento.add(new Paragraph("Alumno: " + alumno.getNombreCompleto(), textoFont));
            documento.add(new Paragraph("Fecha de pase: " + pase.getFecha().format(FECHA), textoFont));
            documento.add(new Paragraph("Institucion destino: " + pase.getInstitucionDestino(), textoFont));
            documento.add(new Paragraph(" "));

            Paragraph subtituloTrayectoria = new Paragraph("Trayectoria institucional", subtituloFont);
            subtituloTrayectoria.setSpacingAfter(10);
            documento.add(subtituloTrayectoria);

            PdfPTable tabla = new PdfPTable(4);
            tabla.setWidthPercentage(100);
            agregarEncabezado(tabla, "Ciclo Lectivo");
            agregarEncabezado(tabla, "Año/Division");
            agregarEncabezado(tabla, "Repitio");
            agregarEncabezado(tabla, "Condicion");

            for (AlumnoCicloLectivo acl : trayectoria) {
                tabla.addCell(new PdfPCell(new com.lowagie.text.Phrase(String.valueOf(acl.getCicloLectivo().getAnio()), textoFont)));
                tabla.addCell(new PdfPCell(new com.lowagie.text.Phrase(acl.getCursoDivision().getNombreCompleto(), textoFont)));
                tabla.addCell(new PdfPCell(new com.lowagie.text.Phrase(acl.isRepetidor() ? "Si" : "No", textoFont)));
                tabla.addCell(new PdfPCell(new com.lowagie.text.Phrase(acl.isCondicionLibre() ? "Condicion Libre" : "Regular", textoFont)));
            }
            documento.add(tabla);

            documento.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("No se pudo generar el PDF de pase: " + e.getMessage());
        }
    }

    private void agregarEncabezado(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new com.lowagie.text.Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        tabla.addCell(celda);
    }
}
