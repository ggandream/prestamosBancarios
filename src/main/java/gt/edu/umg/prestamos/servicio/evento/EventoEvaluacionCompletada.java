package gt.edu.umg.prestamos.servicio.evento;

import java.util.UUID;

/**
 * Se publica cuando el motor de scoring termina de evaluar una solicitud, con el
 * resultado final (APROBADO/RECHAZADO). Permite que otros componentes reaccionen sin
 * acoplarse a la evaluación (patrón Observer).
 */
public record EventoEvaluacionCompletada(UUID prestamoId, int score, String decision) {
}
