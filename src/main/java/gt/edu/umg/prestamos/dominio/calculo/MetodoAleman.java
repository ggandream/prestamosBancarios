package gt.edu.umg.prestamos.dominio.calculo;

import gt.edu.umg.prestamos.dominio.prestamo.Cuota;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Amortización alemana: capital <strong>constante</strong> e interés sobre saldo
 * decreciente, por lo que la cuota total disminuye período a período.
 *
 * <p>Capital por cuota = {@code M / n}. Interés = {@code saldo * i} con {@code i = tasaAnual / 12}.
 * La última cuota amortiza el capital remanente para saldar exactamente la deuda.
 */
public final class MetodoAleman implements CalculadoraInteres {

    private static final int ESCALA = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;
    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal MESES_ANIO = new BigDecimal("12");

    @Override
    public List<Cuota> calcular(BigDecimal monto, int plazoMeses, BigDecimal tasaAnual,
                                LocalDate fechaPrimeraCuota) {
        BigDecimal i = tasaAnual.divide(MESES_ANIO, MC);
        BigDecimal capitalConstante = monto.divide(new BigDecimal(plazoMeses), ESCALA, REDONDEO);

        List<Cuota> cuotas = new ArrayList<>(plazoMeses);
        BigDecimal saldo = monto.setScale(ESCALA, REDONDEO);
        for (int numero = 1; numero <= plazoMeses; numero++) {
            BigDecimal interes = saldo.multiply(i).setScale(ESCALA, REDONDEO);
            BigDecimal capital = (numero == plazoMeses) ? saldo : capitalConstante;
            BigDecimal total = capital.add(interes);
            saldo = saldo.subtract(capital);
            cuotas.add(new Cuota(numero, fechaPrimeraCuota.plusMonths(numero - 1L), capital, interes, total));
        }
        return cuotas;
    }

    @Override
    public String nombre() {
        return "ALEMAN";
    }
}
