package ar.edu.wellspring.sigaiws.modules.edfisica.domain;

/** Seccion 5.7 (extension): SAF = Sin Apto Fisico ese dia puntual (ej. lesion). */
public enum TipoAsistenciaEducacionFisica {
    PRESENTE,
    AUSENTE,
    LLEGADA_TARDE,
    SAF;

    /** SAF cuenta como presente: el alumno va a clase igual, solo no hace actividad fisica. */
    public boolean cuentaComoPresente() {
        return this == PRESENTE || this == SAF;
    }
}
