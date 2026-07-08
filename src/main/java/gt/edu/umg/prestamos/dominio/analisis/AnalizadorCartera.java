package gt.edu.umg.prestamos.dominio.analisis;

import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Aprobado;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Desembolsado;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.EnMora;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pipeline de análisis de cartera con Stream API. Lógica pura: recibe una lista de
 * préstamos y devuelve un {@link ResumenCartera}, sin efectos secundarios ni impresión.
 * Migrado del proyecto de consola del Entregable 1.
 */
public final class AnalizadorCartera {

    private static final int ESCALA = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(ESCALA, REDONDEO);

    public ResumenCartera analizar(List<Prestamo> prestamos) {
        if (prestamos == null || prestamos.isEmpty()) {
            return new ResumenCartera(0, CERO, CERO, CERO, CERO, CERO, Map.of(), Map.of(), Map.of());
        }

        long total = prestamos.size();

        BigDecimal montoTotal = prestamos.stream()
                .map(Prestamo::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(ESCALA, REDONDEO);

        BigDecimal montoPromedio = montoTotal.divide(BigDecimal.valueOf(total), ESCALA, REDONDEO);

        BigDecimal montoMinimo = prestamos.stream()
                .map(Prestamo::getMonto).min(BigDecimal::compareTo).orElse(CERO)
                .setScale(ESCALA, REDONDEO);

        BigDecimal montoMaximo = prestamos.stream()
                .map(Prestamo::getMonto).max(BigDecimal::compareTo).orElse(CERO)
                .setScale(ESCALA, REDONDEO);

        Map<String, Long> conteoPorTipo = prestamos.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getClass().getSimpleName(),
                        Collectors.counting()));

        Map<String, BigDecimal> montoPorTipo = prestamos.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getClass().getSimpleName(),
                        Collectors.reducing(CERO, Prestamo::getMonto, BigDecimal::add)));

        Map<String, Long> conteoPorRiesgo = prestamos.stream()
                .collect(Collectors.groupingBy(
                        AnalizadorCartera::clasificarRiesgo,
                        Collectors.counting()));

        BigDecimal indiceMora = calcularIndiceMora(prestamos);

        return new ResumenCartera(total, montoTotal, montoPromedio, montoMinimo, montoMaximo,
                indiceMora, conteoPorTipo, montoPorTipo, conteoPorRiesgo);
    }

    /** Índice de mora = capital en mora / capital activo (desembolsado + en mora). */
    private static BigDecimal calcularIndiceMora(List<Prestamo> prestamos) {
        BigDecimal capitalEnMora = prestamos.stream()
                .filter(p -> p.getEstado() instanceof EnMora)
                .map(Prestamo::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal capitalActivo = prestamos.stream()
                .filter(p -> p.getEstado() instanceof Desembolsado || p.getEstado() instanceof EnMora)
                .map(Prestamo::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (capitalActivo.signum() == 0) {
            return CERO;
        }
        return capitalEnMora.divide(capitalActivo, MathContext.DECIMAL64).setScale(4, REDONDEO);
    }

    private static String clasificarRiesgo(Prestamo prestamo) {
        return switch (prestamo.getEstado()) {
            case EnMora m -> "ALTO";
            case Desembolsado d -> "MEDIO";
            case Aprobado a -> "BAJO";
            default -> "SIN_RIESGO";
        };
    }
}
