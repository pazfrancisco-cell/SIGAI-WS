package ar.edu.wellspring.sigaiws.modules.calificacion.domain;

import java.time.LocalDate;

/** Mesas de examen para rendir materias adeudadas: diciembre y febrero al cerrar el año, julio para previas. */
public enum TurnoMesa {
    DICIEMBRE,
    FEBRERO,
    JULIO;

    /** El turno en curso o el proximo segun la fecha: enero-abril febrero, mayo-agosto julio, septiembre-diciembre diciembre. */
    public static TurnoMesa segun(LocalDate fecha) {
        int mes = fecha.getMonthValue();
        if (mes >= 9) {
            return DICIEMBRE;
        }
        if (mes <= 4) {
            return FEBRERO;
        }
        return JULIO;
    }
}
