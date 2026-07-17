package gt.edu.umg.prestamos.servicio.evento;

import java.util.UUID;

/**
 * Se publica al crear una solicitud de préstamo. Dispara la evaluación asíncrona en
 * {@code ListenerEvaluacion} (patrón Observer con eventos nativos de Spring).
 */
public record EventoSolicitudCreada(UUID prestamoId) {
}
