package gt.edu.umg.prestamos.servicio;

/**
 * Señala que un recurso solicitado (cliente, préstamo) no existe. La capa API la
 * traduce a HTTP 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
