package gt.edu.umg.prestamos.dominio.calculo;

import gt.edu.umg.prestamos.dominio.prestamo.Cuota;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Estrategia de cálculo del plan de amortización (patrón Strategy). Cada
 * implementación produce el desglose de cuotas para un préstamo dado.
 *
 * <p>El contrato principal de 3 argumentos del Entregable 1
 * ({@link #calcular(BigDecimal, int, BigDecimal)}) delega en la variante con fecha
 * de primera cuota, de modo que el plan pueda fechar las cuotas de forma real y
 * determinista sin acoplar la estrategia a un reloj.
 */
public interface CalculadoraInteres {

    /**
     * Calcula el plan de amortización fechando las cuotas a partir de {@code fechaPrimeraCuota}.
     *
     * @param monto            capital solicitado (&gt; 0)
     * @param plazoMeses       número de cuotas (&gt; 0)
     * @param tasaAnual        tasa nominal anual como fracción decimal (ej. 0.12 = 12%)
     * @param fechaPrimeraCuota fecha de vencimiento de la cuota 1
     */
    List<Cuota> calcular(BigDecimal monto, int plazoMeses, BigDecimal tasaAnual, LocalDate fechaPrimeraCuota);

    /** Variante del Entregable 1: usa como primera fecha el mes siguiente a hoy. */
    default List<Cuota> calcular(BigDecimal monto, int plazoMeses, BigDecimal tasaAnual) {
        return calcular(monto, plazoMeses, tasaAnual, LocalDate.now().plusMonths(1));
    }

    /** Nombre legible de la estrategia (para reportes y detalle de scoring). */
    String nombre();
}
