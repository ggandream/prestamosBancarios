package gt.edu.umg.prestamos.persistencia.entidad;

/**
 * Discriminador persistido del estado de un préstamo. Cada valor corresponde a un
 * record del sealed interface {@code EstadoPrestamo} del dominio. Se guarda como
 * texto en la columna {@code estado_tipo}; el mapper reconstruye el record correcto.
 */
public enum TipoEstado {
    BORRADOR,
    EN_EVALUACION,
    APROBADO,
    RECHAZADO,
    DESEMBOLSADO,
    PAGADO,
    EN_MORA
}
