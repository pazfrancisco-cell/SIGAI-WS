package ar.edu.wellspring.sigaiws.common.exception;

/** Se lanza cuando se busca una entidad por id/clave y no existe. Se traduce en HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String entidad, Object id) {
        return new NotFoundException(entidad + " no encontrado/a (id=" + id + ")");
    }
}
