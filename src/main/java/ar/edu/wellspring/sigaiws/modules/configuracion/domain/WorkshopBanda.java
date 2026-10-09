package ar.edu.wellspring.sigaiws.modules.configuracion.domain;

/** Seccion 5.6: las aulas de workshop se dividen en dos bandas de años. */
public enum WorkshopBanda {
    ANIOS_1_A_3,
    ANIOS_4_A_6;

    public static WorkshopBanda deAnio(int anio) {
        return anio <= 3 ? ANIOS_1_A_3 : ANIOS_4_A_6;
    }
}
