package gt.edu.umg.prestamos.api.dto;

import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoAutomotriz;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoHipotecario;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoPersonal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Representación pública de un préstamo/solicitud.
 *
 * @param estado            nombre del estado actual (Borrador, EnEvaluacion, Aprobado…)
 * @param descripcionEstado descripción legible generada por el dominio
 * @param cuotaEstimada     cuota representativa según el método por defecto del producto
 */
public record RespuestaPrestamoDTO(
        UUID id,
        UUID clienteId,
        String tipo,
        BigDecimal monto,
        int plazoMeses,
        BigDecimal tasaAnual,
        LocalDate fechaSolicitud,
        String estado,
        String descripcionEstado,
        BigDecimal cuotaEstimada) {

    /** Mapea el préstamo de dominio a su representación de API. */
    public static RespuestaPrestamoDTO desde(Prestamo prestamo) {
        String tipo = switch (prestamo) {
            case PrestamoPersonal p -> "PERSONAL";
            case PrestamoHipotecario h -> "HIPOTECARIO";
            case PrestamoAutomotriz a -> "AUTOMOTRIZ";
            default -> throw new IllegalArgumentException(
                    "Subtipo de Prestamo no soportado: " + prestamo.getClass().getName());
        };
        return new RespuestaPrestamoDTO(
                prestamo.getId(), prestamo.getCliente().getId(), tipo,
                prestamo.getMonto(), prestamo.getPlazoMeses(), prestamo.getTasaAnual(),
                prestamo.getFechaSolicitud(),
                prestamo.getEstado().getClass().getSimpleName(),
                prestamo.descripcionEstado(),
                prestamo.calcularCuota());
    }
}
