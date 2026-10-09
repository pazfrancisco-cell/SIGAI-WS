package ar.edu.wellspring.sigaiws.common.exception;

/**
 * Violacion de unicidad de negocio (DNI de alumno/tutor, email institucional, etc.)
 * detectada de forma anticipada en el service, antes de delegar al constraint de BD.
 * Se traduce en HTTP 409 con el mensaje mostrado en el input correspondiente.
 */
public class DuplicateResourceException extends RuntimeException {

    private final String campo;

    public DuplicateResourceException(String campo, String message) {
        super(message);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
