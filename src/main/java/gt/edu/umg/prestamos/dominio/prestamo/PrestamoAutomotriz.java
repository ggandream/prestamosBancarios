package gt.edu.umg.prestamos.dominio.prestamo;

import gt.edu.umg.prestamos.dominio.calculo.CalculadoraInteres;
import gt.edu.umg.prestamos.dominio.calculo.MetodoAleman;
import gt.edu.umg.prestamos.dominio.cliente.Cliente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Préstamo para adquisición de vehículo. Usa amortización alemana (capital constante):
 * amortiza más capital al inicio, acorde con la depreciación del bien financiado.
 */
public final class PrestamoAutomotriz extends Prestamo {

    private final String vehiculo;
    private final BigDecimal depreciacionAnual;

    public PrestamoAutomotriz(UUID id, Cliente cliente, BigDecimal monto, int plazoMeses,
                              BigDecimal tasaAnual, LocalDate fechaSolicitud,
                              String vehiculo, BigDecimal depreciacionAnual) {
        super(id, cliente, monto, plazoMeses, tasaAnual, fechaSolicitud);
        if (vehiculo == null || vehiculo.isBlank()) {
            throw new IllegalArgumentException("vehiculo no puede estar vacío");
        }
        this.vehiculo = vehiculo;
        this.depreciacionAnual = exigirPositivo(depreciacionAnual, "depreciacionAnual");
    }

    @Override
    protected CalculadoraInteres calculadoraPorDefecto() {
        return new MetodoAleman();
    }

    public String getVehiculo() {
        return vehiculo;
    }

    public BigDecimal getDepreciacionAnual() {
        return depreciacionAnual;
    }
}
