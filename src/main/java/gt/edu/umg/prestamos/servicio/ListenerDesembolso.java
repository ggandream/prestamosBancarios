package gt.edu.umg.prestamos.servicio;

import gt.edu.umg.prestamos.servicio.evento.EventoPrestamoDesembolsado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Observador de desembolsos (patrón Observer): procesa
 * {@link EventoPrestamoDesembolsado} en segundo plano simulando la notificación al
 * cliente. Las notificaciones reales (email/SMS) están fuera de alcance (Sección 11
 * del CLAUDE.md), por lo que aquí solo se registra en el log.
 */
@Component
public class ListenerDesembolso {

    private static final Logger log = LoggerFactory.getLogger(ListenerDesembolso.class);

    @Async
    @EventListener
    public void alDesembolsar(EventoPrestamoDesembolsado evento) {
        log.info("notificacion_simulada_desembolso prestamo={} monto={}",
                evento.prestamoId(), evento.monto());
    }
}
