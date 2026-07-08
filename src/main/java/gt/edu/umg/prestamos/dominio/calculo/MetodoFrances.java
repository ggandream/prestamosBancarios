package gt.edu.umg.prestamos.dominio.calculo;

import gt.edu.umg.prestamos.dominio.prestamo.Cuota;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Amortización francesa: cuota total <strong>fija</strong> durante todo el plazo.
 *
 * <p>Fórmula de la cuota: {@code C = M * i / (1 - (1 + i)^-n)} con {@code i = tasaAnual / 12}.
 * El interés de cada período se calcula sobre el saldo pendiente y el capital es el
 * resto de la cuota. La última cuota ajusta el capital para saldar exactamente la deuda.
 */
public final class MetodoFrances implements CalculadoraInteres {

    private static final int ESCALA = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;
    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal MESES_ANIO = new BigDecimal("12");

    @Override
    public List<Cuota> calcular(BigDecimal monto, int plazoMeses, BigDecimal tasaAnual,
                                LocalDate fechaPrimeraCuota) {
        BigDecimal i = tasaAnual.divide(MESES_ANIO, MC);
        BigDecimal factor = BigDecimal.ONE.add(i).pow(plazoMeses, MC);          // (1 + i)^n
        BigDecimal descuento = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(factor, MC)); // 1 - (1+i)^-n
        BigDecimal cuotaFija = monto.multiply(i).divide(descuento, MC).setScale(ESCALA, REDONDEO);

        List<Cuota> cuotas = new ArrayList<>(plazoMeses);
        BigDecimal saldo = monto.setScale(ESCALA, REDONDEO);
        for (int numero = 1; numero <= plazoMeses; numero++) {
            BigDecimal interes = saldo.multiply(i).setScale(ESCALA, REDONDEO);
            BigDecimal capital;
            BigDecimal total;
            if (numero == plazoMeses) {
                capital = saldo;                       // última cuota: saldar el remanente exacto
                total = capital.add(interes);
            } else {
                capital = cuotaFija.subtract(interes);
                total = cuotaFija;
            }
            saldo = saldo.subtract(capital);
            cuotas.add(new Cuota(numero, fechaPrimeraCuota.plusMonths(numero - 1L), capital, interes, total));
        }
        return cuotas;
    }

    @Override
    public String nombre() {
        return "FRANCES";
    }
}
