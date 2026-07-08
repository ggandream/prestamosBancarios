package gt.edu.umg.prestamos.dominio.cliente;

import java.math.BigDecimal;

/**
 * Sector de industria de un {@link ClienteEmpresarial}. El factor de ajuste modela
 * el riesgo/estabilidad del sector al calcular la capacidad de pago empresarial.
 */
public enum SectorIndustria {

    COMERCIO(new BigDecimal("1.00")),
    INDUSTRIA(new BigDecimal("1.00")),
    SERVICIOS(new BigDecimal("0.95")),
    AGRICOLA(new BigDecimal("0.85")),
    CONSTRUCCION(new BigDecimal("0.80"));

    private final BigDecimal factorAjuste;

    SectorIndustria(BigDecimal factorAjuste) {
        this.factorAjuste = factorAjuste;
    }

    public BigDecimal factorAjuste() {
        return factorAjuste;
    }
}
