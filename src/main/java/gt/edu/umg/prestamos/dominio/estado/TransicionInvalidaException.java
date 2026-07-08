package gt.edu.umg.prestamos.dominio.estado;

/**
 * Excepción de dominio lanzada cuando se intenta una transición de estado no
 * permitida por la máquina de estados del préstamo.
 */
public class TransicionInvalidaException extends RuntimeException {

    public TransicionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
