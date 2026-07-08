package gt.edu.umg.prestamos.dominio.prestamo;

import gt.edu.umg.prestamos.dominio.calculo.CalculadoraInteres;
import gt.edu.umg.prestamos.dominio.calculo.MetodoFrances;
import gt.edu.umg.prestamos.dominio.cliente.Cliente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Préstamo con garantía hipotecaria. El monto no puede superar el 80% del avalúo
 * del inmueble. Usa amortización francesa (cuota fija), habitual en créditos de vivienda.
 */
public final class PrestamoHipotecario extends Prestamo {

    /** Máximo financiable como fracción del avalúo (loan-to-value). */
    private static final BigDecimal LTV_MAX = new BigDecimal("0.80");

    private final String descripcionGarantia;
    private final BigDecimal avaluo;

    public PrestamoHipotecario(UUID id, Cliente cliente, BigDecimal monto, int plazoMeses,
                               BigDecimal tasaAnual, LocalDate fechaSolicitud,
                               String descripcionGarantia, BigDecimal avaluo) {
        super(id, cliente, monto, plazoMeses, tasaAnual, fechaSolicitud);
        this.descripcionGarantia = exigirNoVacio(descripcionGarantia, "descripcionGarantia");
        this.avaluo = exigirPositivo(avaluo, "avaluo");
        if (monto.compareTo(avaluo.multiply(LTV_MAX)) > 0) {
            throw new IllegalArgumentException(
                    "monto no puede superar el 80% del avalúo (" + avaluo.multiply(LTV_MAX) + ")");
        }
    }

    private static String exigirNoVacio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede estar vacío");
        }
        return valor;
    }

    @Override
    protected CalculadoraInteres calculadoraPorDefecto() {
        return new MetodoFrances();
    }

    public String getDescripcionGarantia() {
        return descripcionGarantia;
    }

    public BigDecimal getAvaluo() {
        return avaluo;
    }
}
