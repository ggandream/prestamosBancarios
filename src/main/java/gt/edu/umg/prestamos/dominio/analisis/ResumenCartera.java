package gt.edu.umg.prestamos.dominio.analisis;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Resultado del análisis de la cartera de préstamos. Estructura pura de datos; el
 * formato del reporte es responsabilidad de la capa API (Fase 4).
 *
 * @param totalPrestamos  cantidad de préstamos analizados
 * @param montoTotal      suma de los montos
 * @param montoPromedio   promedio de los montos
 * @param montoMinimo     monto más bajo
 * @param montoMaximo     monto más alto
 * @param indiceMora      fracción del capital activo que está en mora [0, 1]
 * @param conteoPorTipo   cantidad de préstamos por tipo de producto
 * @param montoPorTipo    monto acumulado por tipo de producto
 * @param conteoPorRiesgo cantidad de préstamos por nivel de riesgo
 */
public record ResumenCartera(
        long totalPrestamos,
        BigDecimal montoTotal,
        BigDecimal montoPromedio,
        BigDecimal montoMinimo,
        BigDecimal montoMaximo,
        BigDecimal indiceMora,
        Map<String, Long> conteoPorTipo,
        Map<String, BigDecimal> montoPorTipo,
        Map<String, Long> conteoPorRiesgo) {

    public ResumenCartera {
        conteoPorTipo = Map.copyOf(conteoPorTipo);
        montoPorTipo = Map.copyOf(montoPorTipo);
        conteoPorRiesgo = Map.copyOf(conteoPorRiesgo);
    }
}
