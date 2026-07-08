package gt.edu.umg.prestamos.dominio.cliente;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Cliente persona natural. Su capacidad de pago es el 35% del salario mensual,
 * ajustado por la estabilidad de su {@link TipoEmpleo}.
 */
public final class ClienteIndividual extends Cliente {

    /** Porcentaje del salario considerado comprometible en cuota. */
    private static final BigDecimal FACTOR_CAPACIDAD = new BigDecimal("0.35");

    private final BigDecimal salarioMensual;
    private final TipoEmpleo tipoEmpleo;
    private final int antiguedadLaboral;

    public ClienteIndividual(UUID id, String nombre, String documento, String email,
                             LocalDate fechaRegistro, HistorialCrediticio historial,
                             BigDecimal salarioMensual, TipoEmpleo tipoEmpleo, int antiguedadLaboral) {
        super(id, nombre, documento, email, fechaRegistro, historial);
        this.salarioMensual = exigirPositivo(salarioMensual, "salarioMensual");
        this.tipoEmpleo = java.util.Objects.requireNonNull(tipoEmpleo, "tipoEmpleo no puede ser null");
        if (antiguedadLaboral < 0) {
            throw new IllegalArgumentException("antiguedadLaboral no puede ser negativa");
        }
        this.antiguedadLaboral = antiguedadLaboral;
    }

    @Override
    public BigDecimal getCapacidadPago() {
        return salarioMensual
                .multiply(FACTOR_CAPACIDAD)
                .multiply(tipoEmpleo.factorAjuste())
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getIngresoMensual() {
        return salarioMensual.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public int getAntiguedadAnios() {
        return antiguedadLaboral;
    }

    public BigDecimal getSalarioMensual() {
        return salarioMensual;
    }

    public TipoEmpleo getTipoEmpleo() {
        return tipoEmpleo;
    }

    public int getAntiguedadLaboral() {
        return antiguedadLaboral;
    }
}
