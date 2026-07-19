package gt.edu.umg.prestamos.api.dto;

import gt.edu.umg.prestamos.servicio.AmortizacionService.PlanAmortizacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Plan de amortización completo de un préstamo.
 *
 * @param metodo         método de amortización aplicado (FRANCES/ALEMAN)
 * @param totalIntereses suma de los intereses de todas las cuotas
 * @param totalPagar     capital + intereses
 */
public record PlanPagosDTO(
        UUID prestamoId,
        String metodo,
        BigDecimal monto,
        int plazoMeses,
        BigDecimal totalIntereses,
        BigDecimal totalPagar,
        List<CuotaDTO> cuotas) {

    /** Mapea el resultado del servicio de amortización a su representación de API. */
    public static PlanPagosDTO desde(PlanAmortizacion plan) {
        BigDecimal totalIntereses = plan.cuotas().stream()
                .map(c -> c.interes())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPagar = plan.cuotas().stream()
                .map(c -> c.total())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return new PlanPagosDTO(
                plan.prestamo().getId(), plan.metodo(), plan.prestamo().getMonto(),
                plan.prestamo().getPlazoMeses(), totalIntereses, totalPagar,
                plan.cuotas().stream().map(CuotaDTO::desde).toList());
    }
}
