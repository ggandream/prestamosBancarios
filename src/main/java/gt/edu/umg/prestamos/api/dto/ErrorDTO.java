package gt.edu.umg.prestamos.api.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de error uniforme de la API.
 *
 * @param status   código HTTP
 * @param error    nombre corto del error (Not Found, Bad Request, Conflict…)
 * @param mensaje  descripción del problema
 * @param detalles detalle por campo en errores de validación (vacío en el resto)
 */
public record ErrorDTO(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        List<String> detalles) {

    public static ErrorDTO de(int status, String error, String mensaje, List<String> detalles) {
        return new ErrorDTO(LocalDateTime.now(), status, error, mensaje, detalles);
    }
}
