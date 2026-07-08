package gt.edu.umg.prestamos.dominio.prestamo;

import gt.edu.umg.prestamos.dominio.calculo.CalculadoraInteres;
import gt.edu.umg.prestamos.dominio.calculo.MetodoFrances;
import gt.edu.umg.prestamos.dominio.cliente.Cliente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Préstamo de consumo sin garantía. Usa amortización francesa (cuota fija), lo
 * habitual en este producto.
 */
public final class PrestamoPersonal extends Prestamo {

    public PrestamoPersonal(UUID id, Cliente cliente, BigDecimal monto, int plazoMeses,
                            BigDecimal tasaAnual, LocalDate fechaSolicitud) {
        super(id, cliente, monto, plazoMeses, tasaAnual, fechaSolicitud);
    }

    @Override
    protected CalculadoraInteres calculadoraPorDefecto() {
        return new MetodoFrances();
    }
}
