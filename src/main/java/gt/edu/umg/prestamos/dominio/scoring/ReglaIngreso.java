package gt.edu.umg.prestamos.dominio.scoring;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;

import java.math.BigDecimal;

/**
 * Evalúa el ingreso mensual del cliente contra umbrales fijos. Peso 25.
 *
 * <p>Brackets (en la moneda del sistema): &ge; 5000 → 100 · &ge; 3000 → 60 ·
 * &ge; 1500 → 30 · resto → 0.
 */
public final class ReglaIngreso implements ReglaScoring {

    private static final int PESO = 25;
    private static final BigDecimal U1 = new BigDecimal("5000");
    private static final BigDecimal U2 = new BigDecimal("3000");
    private static final BigDecimal U3 = new BigDecimal("1500");

    @Override
    public int evaluar(Cliente cliente, Prestamo prestamo) {
        BigDecimal ingreso = cliente.getIngresoMensual();
        if (ingreso.compareTo(U1) >= 0) return 100;
        if (ingreso.compareTo(U2) >= 0) return 60;
        if (ingreso.compareTo(U3) >= 0) return 30;
        return 0;
    }

    @Override
    public int peso() {
        return PESO;
    }

    @Override
    public String descripcion() {
        return "Ingreso mensual vs. umbral mínimo";
    }
}
