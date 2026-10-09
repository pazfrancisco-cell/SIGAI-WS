package ar.edu.wellspring.sigaiws.modules.horario.service;

import ar.edu.wellspring.sigaiws.common.pdf.PdfTableGenerator;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.PeriodoHorario;
import ar.edu.wellspring.sigaiws.modules.horario.domain.HorarioSlot;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Seccion 5.5.4: "Boton de exportacion a PDF para imprimir" (Mi Horario). */
@Component
@RequiredArgsConstructor
public class HorarioPdfGenerator {

    private static final String[] DIAS = {"Lunes", "Martes", "Miercoles", "Jueves", "Viernes"};

    private final PdfTableGenerator pdfTableGenerator;

    public byte[] generarMiHorario(Usuario docente, List<PeriodoHorario> periodos, List<HorarioSlot> slots) {
        List<String> columnas = new ArrayList<>();
        columnas.add("Periodo");
        for (String dia : DIAS) columnas.add(dia);

        List<List<String>> filas = new ArrayList<>();
        for (PeriodoHorario periodo : periodos) {
            List<String> fila = new ArrayList<>();
            fila.add(periodo.getNombre());
            for (int dia = 1; dia <= 5; dia++) {
                int diaFinal = dia;
                var slot = slots.stream()
                        .filter(s -> s.getDiaSemana() == diaFinal && s.getPeriodoOrden().equals(periodo.getOrden()))
                        .findFirst();
                if (slot.isPresent() && slot.get().getMateria() != null) {
                    var s = slot.get();
                    String texto = s.getMateria().getNombre() + " - " + s.getCursoDivision().getNombreCompleto();
                    if (s.getAula() != null) texto += " (" + s.getAula().getNombre() + ")";
                    fila.add(texto);
                } else {
                    fila.add("");
                }
            }
            filas.add(fila);
        }

        return pdfTableGenerator.generar("Mi Horario", List.of("Docente: " + docente.getNombreCompleto()), columnas, filas);
    }
}
