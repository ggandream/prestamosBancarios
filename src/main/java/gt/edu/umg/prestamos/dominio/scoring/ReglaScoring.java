package gt.edu.umg.prestamos.dominio.scoring;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;

/**
 * Regla de scoring (patrón Strategy). Cada regla evalúa un aspecto de la solicitud y
 * devuelve un puntaje de 0 a 100. El {@link MotorScoring} las combina por peso.
 *
 * <p>El conjunto de reglas es <strong>cerrado</strong> (ver Sección 4 del CLAUDE.md):
 * {@code ReglaCapacidadPago}, {@code ReglaIngreso}, {@code ReglaHistorial}, {@code ReglaEdad}.
 */
public interface ReglaScoring {

    /** Puntaje de la regla, entero en el rango [0, 100]. */
    int evaluar(Cliente cliente, Prestamo prestamo);

    /** Peso fijo y constante de la regla en el promedio ponderado (los pesos suman 100). */
    int peso();

    /** Descripción legible de la regla (para el detalle del resultado). */
    String descripcion();
}
