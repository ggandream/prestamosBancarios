package gt.edu.umg.prestamos.dominio.scoring;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Evalúa la relación cuota estimada / capacidad de pago mensual. Cuanto menor es el
 * ratio, mejor el puntaje. Peso 40 (la regla de mayor importancia).
 *
 * <p>Brackets: ratio &le; 0.30 → 100 · &le; 0.40 → 70 · &le; 0.50 → 40 · resto → 0.
 */
public final class ReglaCapacidadPago implements ReglaScoring {

    private static final int PESO = 40;
    private static final BigDecimal T1 = new BigDecimal("0.30");
    private static final BigDecimal T2 = new BigDecimal("0.40");
    private static final BigDecimal T3 = new BigDecimal("0.50");

    @Override
    public int evaluar(Cliente cliente, Prestamo prestamo) {
        BigDecimal capacidad = cliente.getCapacidadPago();
        if (capacidad.signum() <= 0) {
            return 0;
        }
        BigDecimal ratio = prestamo.calcularCuota().divide(capacidad, MathContext.DECIMAL64);
        if (ratio.compareTo(T1) <= 0) return 100;
        if (ratio.compareTo(T2) <= 0) return 70;
        if (ratio.compareTo(T3) <= 0) return 40;
        return 0;
    }

    @Override
    public int peso() {
        return PESO;
    }

    @Override
    public String descripcion() {
        return "Capacidad de pago (cuota/capacidad mensual)";
    }
}
