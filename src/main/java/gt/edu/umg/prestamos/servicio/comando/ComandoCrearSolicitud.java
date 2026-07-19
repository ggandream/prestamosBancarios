package gt.edu.umg.prestamos.servicio.comando;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando de creación de solicitud de préstamo. Interfaz sellada: cada variante lleva
 * los datos propios de su producto y {@code SolicitudService} instancia el subtipo de
 * {@code Prestamo} con un switch exhaustivo.
 */
public sealed interface ComandoCrearSolicitud {

    UUID clienteId();

    BigDecimal monto();

    int plazoMeses();

    BigDecimal tasaAnual();

    /** Solicitud de {@code PrestamoPersonal}. */
    record Personal(UUID clienteId, BigDecimal monto, int plazoMeses, BigDecimal tasaAnual)
            implements ComandoCrearSolicitud {}

    /** Solicitud de {@code PrestamoHipotecario}. */
    record Hipotecaria(UUID clienteId, BigDecimal monto, int plazoMeses, BigDecimal tasaAnual,
                       String descripcionGarantia, BigDecimal avaluo)
            implements ComandoCrearSolicitud {}

    /** Solicitud de {@code PrestamoAutomotriz}. */
    record Automotriz(UUID clienteId, BigDecimal monto, int plazoMeses, BigDecimal tasaAnual,
                      String vehiculo, BigDecimal depreciacionAnual)
            implements ComandoCrearSolicitud {}
}
