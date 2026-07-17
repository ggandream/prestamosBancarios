package gt.edu.umg.prestamos.api.dto;

import gt.edu.umg.prestamos.servicio.EvaluacionService.EvaluacionCompletada;

import java.util.List;
import java.util.UUID;

/**
 * Resultado de la evaluación de una solicitud.
 *
 * @param decision APROBADO o RECHAZADO
 * @param estado   nombre del estado final del préstamo
 * @param detalle  desglose del aporte de cada regla de scoring
 */
public record ResultadoEvaluacionDTO(
        UUID prestamoId,
        int score,
        String decision,
        String estado,
        List<String> detalle) {

    /** Mapea el resultado del servicio de evaluación a su representación de API. */
    public static ResultadoEvaluacionDTO desde(EvaluacionCompletada evaluacion) {
        return new ResultadoEvaluacionDTO(
                evaluacion.prestamo().getId(),
                evaluacion.resultado().score(),
                evaluacion.resultado().decision(),
                evaluacion.prestamo().getEstado().getClass().getSimpleName(),
                evaluacion.resultado().detalle());
    }
}
