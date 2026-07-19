package gt.edu.umg.prestamos.servicio.evento;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Se publica al desembolsar un préstamo aprobado. Lo procesa
 * {@code ListenerDesembolso} (notificación simulada, patrón Observer).
 */
public record EventoPrestamoDesembolsado(UUID prestamoId, BigDecimal monto) {
}
