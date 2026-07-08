package gt.edu.umg.prestamos.dominio.cliente;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Cliente persona jurídica. Su capacidad de pago es el 10% de la facturación
 * mensualizada (facturación anual / 12), ajustada por el riesgo del {@link SectorIndustria}.
 */
public final class ClienteEmpresarial extends Cliente {

    private static final BigDecimal MESES_ANIO = new BigDecimal("12");
    /** Porcentaje de la facturación mensual considerado comprometible en cuota. */
    private static final BigDecimal FACTOR_CAPACIDAD = new BigDecimal("0.10");

    private final BigDecimal facturacionAnual;
    private final String nit;
    private final SectorIndustria sector;
    private final int antiguedadNit;

    public ClienteEmpresarial(UUID id, String nombre, String documento, String email,
                              LocalDate fechaRegistro, HistorialCrediticio historial,
                              BigDecimal facturacionAnual, String nit, SectorIndustria sector,
                              int antiguedadNit) {
        super(id, nombre, documento, email, fechaRegistro, historial);
        this.facturacionAnual = exigirPositivo(facturacionAnual, "facturacionAnual");
        this.nit = exigirNoVacio(nit, "nit");
        this.sector = Objects.requireNonNull(sector, "sector no puede ser null");
        if (antiguedadNit < 0) {
            throw new IllegalArgumentException("antiguedadNit no puede ser negativa");
        }
        this.antiguedadNit = antiguedadNit;
    }

    @Override
    public BigDecimal getCapacidadPago() {
        return getIngresoMensual()
                .multiply(FACTOR_CAPACIDAD)
                .multiply(sector.factorAjuste())
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getIngresoMensual() {
        return facturacionAnual.divide(MESES_ANIO, MathContext.DECIMAL64)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public int getAntiguedadAnios() {
        return antiguedadNit;
    }

    public BigDecimal getFacturacionAnual() {
        return facturacionAnual;
    }

    public String getNit() {
        return nit;
    }

    public SectorIndustria getSector() {
        return sector;
    }

    public int getAntiguedadNit() {
        return antiguedadNit;
    }
}
