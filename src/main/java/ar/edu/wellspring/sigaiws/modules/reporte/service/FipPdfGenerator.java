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

/**
 * FIP (Ficha de Informacion Pedagogica), seccion 5.8 (extension, no contemplada
 * en la Propuesta Tecnica original). Resumen pensado para un psicopedagogo o
 * Equipo de Orientacion Escolar: materias con dificultades, observaciones
 * disciplinarias, restricciones medicas/apto fisico, y observaciones
 * pedagogicas cargadas a mano al generarla.
 */
@Component
public class FipPdfGenerator {

    public record Tutor(String nombre, String vinculo, String telefono, String email) {}
    public record MateriaDificultad(String materia, String promedio) {}
    public record Observacion(String fecha, String categoria, String descripcion) {}
    public record Restriccion(String descripcion, String vigencia) {}

    public byte[] generar(Alumno alumno, CicloLectivo ciclo, String cursoDivision, List<Tutor> tutores,
                           List<MateriaDificultad> materiasConDificultad, boolean condicionLibre,
                           List<Observacion> observaciones, List<Restriccion> restricciones,
                           String estadoAptoFisico, String observacionesPedagogicas) {
        Document documento = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(documento, out);
            documento.open();

            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font subtituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font seccionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font textoFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font encabezadoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

            Paragraph institucion = new Paragraph("Wellspring School", tituloFont);
            institucion.setAlignment(Element.ALIGN_CENTER);
            documento.add(institucion);

            Paragraph titulo = new Paragraph("Ficha de Información Pedagógica (FIP)", subtituloFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(20);
            documento.add(titulo);

            documento.add(new Paragraph("Alumno: " + alumno.getNombreCompleto(), textoFont));
            documento.add(new Paragraph("DNI: " + alumno.getDni(), textoFont));
            documento.add(new Paragraph("Fecha de nacimiento: " + alumno.getFechaNacimiento(), textoFont));
            documento.add(new Paragraph("Curso/División: " + cursoDivision, textoFont));
            documento.add(new Paragraph("Ciclo lectivo: " + ciclo.getAnio(), textoFont));
            documento.add(new Paragraph(" "));

            seccion(documento, "Tutores", seccionFont);
            if (tutores.isEmpty()) {
                documento.add(new Paragraph("Sin tutores registrados.", textoFont));
            } else {
                for (Tutor t : tutores) {
                    documento.add(new Paragraph("• " + t.nombre() + " (" + t.vinculo() + ") — Tel: " + t.telefono() + " — Email: " + t.email(), textoFont));
                }
            }

            seccion(documento, "Materias con dificultades", seccionFont);
            if (condicionLibre) {
                documento.add(new Paragraph("El alumno se encuentra en Condición Libre en el ciclo lectivo actual.", textoFont));
            }
            if (materiasConDificultad.isEmpty()) {
                documento.add(new Paragraph("Sin materias con promedio por debajo de la nota mínima de aprobación.", textoFont));
            } else {
                PdfPTable tabla = new PdfPTable(2);
                tabla.setWidthPercentage(100);
                for (String encabezado : new String[]{"Materia", "Promedio Anual"}) {
                    PdfPCell celda = new PdfPCell(new Phrase(encabezado, encabezadoFont));
                    celda.setGrayFill(0.85f);
                    tabla.addCell(celda);
                }
                for (MateriaDificultad m : materiasConDificultad) {
                    tabla.addCell(new PdfPCell(new Phrase(m.materia(), textoFont)));
                    tabla.addCell(new PdfPCell(new Phrase(m.promedio(), textoFont)));
                }
                documento.add(tabla);
            }

            seccion(documento, "Observaciones disciplinarias relevantes (ciclo actual)", seccionFont);
            if (observaciones.isEmpty()) {
                documento.add(new Paragraph("Sin observaciones disciplinarias en el ciclo lectivo actual.", textoFont));
            } else {
                for (Observacion o : observaciones) {
                    documento.add(new Paragraph("• " + o.fecha() + " (" + o.categoria() + "): " + o.descripcion(), textoFont));
                }
            }

            seccion(documento, "Restricciones médicas y apto físico", seccionFont);
            documento.add(new Paragraph("Apto físico: " + estadoAptoFisico, textoFont));
            if (restricciones.isEmpty()) {
                documento.add(new Paragraph("Sin restricciones médicas registradas.", textoFont));
            } else {
                for (Restriccion r : restricciones) {
                    documento.add(new Paragraph("• " + r.descripcion() + " (" + r.vigencia() + ")", textoFont));
                }
            }

            seccion(documento, "Observaciones pedagógicas generales", seccionFont);
            documento.add(new Paragraph(
                    (observacionesPedagogicas == null || observacionesPedagogicas.isBlank())
                            ? "Sin observaciones cargadas." : observacionesPedagogicas,
                    textoFont));

            documento.add(new Paragraph(" "));
            documento.add(new Paragraph("Firma del responsable del Equipo de Orientación Escolar: ______________________", textoFont));

            documento.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("No se pudo generar la FIP: " + e.getMessage());
        }
    }

    private void seccion(Document documento, String titulo, Font font) throws Exception {
        Paragraph p = new Paragraph(titulo, font);
        p.setSpacingBefore(14);
        p.setSpacingAfter(6);
        documento.add(p);
    }
}
