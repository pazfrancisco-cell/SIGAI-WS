package ar.edu.wellspring.sigaiws.common.exception;

/**
 * Violacion de una regla de negocio explicita de la Propuesta Tecnica
 * (por ejemplo: intentar reasignar el mismo workshop en C1 y C2, o combinar
 * roles no permitidos). Se traduce en HTTP 422 / mensaje de error en el formulario.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
