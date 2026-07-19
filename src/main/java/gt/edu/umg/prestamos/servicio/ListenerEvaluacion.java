package gt.edu.umg.prestamos.servicio;

import gt.edu.umg.prestamos.servicio.evento.EventoEvaluacionCompletada;
import gt.edu.umg.prestamos.servicio.evento.EventoSolicitudCreada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Observador de solicitudes creadas (patrón Observer con eventos nativos de Spring):
 * al recibir {@link EventoSolicitudCreada} ejecuta el motor de scoring en segundo
 * plano — la respuesta HTTP 202 ya se devolvió — y publica
 * {@link EventoEvaluacionCompletada} con el resultado. El estado del préstamo
 * transiciona Borrador → EnEvaluacion → Aprobado/Rechazado sin bloquear al cliente.
 */
@Component
public class ListenerEvaluacion {

    private static final Logger log = LoggerFactory.getLogger(ListenerEvaluacion.class);

    private final EvaluacionService evaluacion;
    private final ApplicationEventPublisher publicador;

    public ListenerEvaluacion(EvaluacionService evaluacion, ApplicationEventPublisher publicador) {
        this.evaluacion = evaluacion;
        this.publicador = publicador;
    }

    @Async
    @EventListener
    public void alCrearSolicitud(EventoSolicitudCreada evento) {
        log.info("evaluacion_asincrona_iniciada prestamo={}", evento.prestamoId());
        var completada = evaluacion.evaluar(evento.prestamoId());
        publicador.publishEvent(new EventoEvaluacionCompletada(
                evento.prestamoId(),
                completada.resultado().score(),
                completada.resultado().decision()));
    }
}
