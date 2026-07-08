package gt.edu.umg.prestamos.dominio.prestamo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una cuota del plan de amortización. Inmutable.
 *
 * @param numero   número de cuota (1-based)
 * @param fechaPago fecha en que vence la cuota
 * @param capital  porción de capital amortizado en la cuota
 * @param interes  porción de interés de la cuota
 * @param total    capital + interés
 */
public record Cuota(int numero, LocalDate fechaPago, BigDecimal capital, BigDecimal interes, BigDecimal total) {
}
