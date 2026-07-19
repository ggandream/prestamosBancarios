package gt.edu.umg.prestamos.api.dto;

import gt.edu.umg.prestamos.dominio.analisis.ResumenCartera;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Reporte de cartera para la API. El cálculo lo hace el dominio
 * ({@link ResumenCartera}); aquí solo se agrega el formato y las conclusiones
 * legibles (responsabilidad de la capa API).
 */
public record ResumenCarteraDTO(
        long totalPrestamos,
        BigDecimal montoTotal,
        BigDecimal montoPromedio,
        BigDecimal montoMinimo,
        BigDecimal montoMaximo,
        BigDecimal indiceMora,
        Map<String, Long> conteoPorTipo,
        Map<String, BigDecimal> montoPorTipo,
        Map<String, Long> conteoPorRiesgo,
        List<String> conclusiones) {

    /** Mapea el resumen de dominio y genera las conclusiones del reporte. */
    public static ResumenCarteraDTO desde(ResumenCartera r) {
        return new ResumenCarteraDTO(
                r.totalPrestamos(), r.montoTotal(), r.montoPromedio(), r.montoMinimo(),
                r.montoMaximo(), r.indiceMora(), r.conteoPorTipo(), r.montoPorTipo(),
                r.conteoPorRiesgo(), generarConclusiones(r));
    }

    private static List<String> generarConclusiones(ResumenCartera r) {
        if (r.totalPrestamos() == 0) {
            return List.of("No hay préstamos registrados en la cartera.");
        }
        List<String> conclusiones = new ArrayList<>();
        conclusiones.add("La cartera tiene %d préstamos por un monto total de %s."
                .formatted(r.totalPrestamos(), r.montoTotal()));
        conclusiones.add("El índice de mora es %s%% del capital activo."
                .formatted(r.indiceMora()
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP)));
        r.montoPorTipo().entrySet().stream()
                .max(Comparator.comparing(Map.Entry::getValue))
                .ifPresent(mayor -> conclusiones.add(
                        "El producto con mayor exposición es %s con %s."
                                .formatted(mayor.getKey(), mayor.getValue())));
        long enRiesgoAlto = r.conteoPorRiesgo().getOrDefault("ALTO", 0L);
        conclusiones.add(enRiesgoAlto == 0
                ? "No hay préstamos en riesgo alto."
                : "Hay %d préstamo(s) en riesgo alto (en mora).".formatted(enRiesgoAlto));
        return conclusiones;
    }
}
