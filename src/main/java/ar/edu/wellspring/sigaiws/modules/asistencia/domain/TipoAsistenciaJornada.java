package ar.edu.wellspring.sigaiws.modules.asistencia.domain;

import java.math.BigDecimal;

/**
 * Seccion 5.2.2: tipos de inasistencia con su equivalencia en faltas.
 * AUSENTE y AUSENTE_JUSTIFICADO valen 0.5 porque se registran POR TURNO
 * (manana/tarde, ver AsistenciaJornada.turno): faltar un solo turno es media
 * falta, y faltar ambos turnos del dia (dos registros) suma la falta completa.
 */
public enum TipoAsistenciaJornada {
    PRESENTE(BigDecimal.ZERO),
    AUSENTE(new BigDecimal("0.5")),
    AUSENTE_JUSTIFICADO(new BigDecimal("0.5")),
    LLEGADA_TARDE(new BigDecimal("0.25")),
    UNIFORME_INCOMPLETO(new BigDecimal("0.25")),
    MEDIA_FALTA(new BigDecimal("0.5"));

    private final BigDecimal equivalencia;

    TipoAsistenciaJornada(BigDecimal equivalencia) {
        this.equivalencia = equivalencia;
    }

    public BigDecimal getEquivalencia() {
        return equivalencia;
    }
}
