package gt.edu.umg.prestamos.api.dto;

import gt.edu.umg.prestamos.servicio.comando.ComandoCrearSolicitud;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request de creación de solicitud de préstamo. Campo discriminador
 * {@code tipoPrestamo}; los campos de garantía/vehículo solo aplican a su producto.
 *
 * @param tipoPrestamo        PERSONAL, HIPOTECARIO o AUTOMOTRIZ
 * @param tasaAnual           fracción decimal (ej. 0.12 = 12%)
 * @param descripcionGarantia solo HIPOTECARIO
 * @param avaluo              solo HIPOTECARIO
 * @param vehiculo            solo AUTOMOTRIZ
 * @param depreciacionAnual   solo AUTOMOTRIZ
 */
public record SolicitudPrestamoDTO(
        @NotNull UUID clienteId,
        @NotBlank @Pattern(regexp = "PERSONAL|HIPOTECARIO|AUTOMOTRIZ") String tipoPrestamo,
        @NotNull @Positive BigDecimal monto,
        @Min(6) @Max(360) int plazoMeses,
        @NotNull @Positive BigDecimal tasaAnual,
        String descripcionGarantia,
        BigDecimal avaluo,
        String vehiculo,
        BigDecimal depreciacionAnual) {

    /** Traduce el request al comando de la capa de servicio. */
    public ComandoCrearSolicitud aComando() {
        return switch (tipoPrestamo) {
            case "PERSONAL" -> new ComandoCrearSolicitud.Personal(
                    clienteId, monto, plazoMeses, tasaAnual);
            case "HIPOTECARIO" -> new ComandoCrearSolicitud.Hipotecaria(
                    clienteId, monto, plazoMeses, tasaAnual, descripcionGarantia, avaluo);
            case "AUTOMOTRIZ" -> new ComandoCrearSolicitud.Automotriz(
                    clienteId, monto, plazoMeses, tasaAnual, vehiculo, depreciacionAnual);
            default -> throw new IllegalArgumentException(
                    "Tipo de préstamo no soportado: " + tipoPrestamo);
        };
    }
}
