package gt.edu.umg.prestamos.dominio.cliente;

import java.math.BigDecimal;

/**
 * Tipo de empleo de un {@link ClienteIndividual}. El factor de ajuste modela la
 * estabilidad del ingreso al calcular la capacidad de pago: a mayor estabilidad,
 * mayor porcentaje del salario se considera comprometible.
 */
public enum TipoEmpleo {

    FORMAL(new BigDecimal("1.00")),
    INDEPENDIENTE(new BigDecimal("0.90")),
    INFORMAL(new BigDecimal("0.75"));

    private final BigDecimal factorAjuste;

    TipoEmpleo(BigDecimal factorAjuste) {
        this.factorAjuste = factorAjuste;
    }

    public BigDecimal factorAjuste() {
        return factorAjuste;
    }
}
