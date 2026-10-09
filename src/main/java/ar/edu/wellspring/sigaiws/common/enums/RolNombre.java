package ar.edu.wellspring.sigaiws.common.enums;

/**
 * Roles institucionales definidos en la seccion 5.12.2 de la Propuesta Tecnica.
 * Las combinaciones validas de roles por usuario se validan en
 * {@code UsuarioService} (seccion 5.12.3): Preceptor+Administrativo,
 * Administrativo+Preceptor, Directivo+Administrativo, Directivo+Preceptor.
 * Docente no se combina con ningun otro rol.
 */
public enum RolNombre {
    SUPERADMIN("Superadministrador"),
    DIRECTIVO("Directivo"),
    ADMINISTRATIVO("Administrativo"),
    PRECEPTOR("Preceptor"),
    DOCENTE("Docente"),
    PADRE_TUTOR("Padre/Tutor");

    private final String etiqueta;

    RolNombre(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
