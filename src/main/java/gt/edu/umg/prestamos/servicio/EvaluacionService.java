package gt.edu.umg.prestamos.servicio;

import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;
import gt.edu.umg.prestamos.dominio.scoring.MotorScoring;
import gt.edu.umg.prestamos.dominio.scoring.ResultadoEvaluacion;
import gt.edu.umg.prestamos.persistencia.adaptador.PrestamoRepositorioJpa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servicio de aplicación que orquesta la evaluación de una solicitud con el
 * {@link MotorScoring} del dominio (patrón Strategy: el motor recibe las reglas como
 * beans, ver {@code ScoringConfig}). Transiciona el préstamo
 * Borrador → EnEvaluacion → Aprobado/Rechazado según la decisión del motor.
 */
@Service
public class EvaluacionService {

    /** Identificador del evaluador registrado en el estado EnEvaluacion. */
    static final String EVALUADOR = "motor-scoring";

    private static final Logger log = LoggerFactory.getLogger(EvaluacionService.class);

    private final PrestamoRepositorioJpa prestamos;
    private final MotorScoring motor;

    public EvaluacionService(PrestamoRepositorioJpa prestamos, MotorScoring motor) {
        this.prestamos = prestamos;
        this.motor = motor;
    }

    /** Préstamo ya evaluado junto con el detalle del scoring. */
    public record EvaluacionCompletada(Prestamo prestamo, ResultadoEvaluacion resultado) {}

    /**
     * Evalúa una solicitud en estado Borrador y persiste el estado final.
     *
     * @throws RecursoNoEncontradoException si el préstamo no existe
     * @throws gt.edu.umg.prestamos.dominio.estado.TransicionInvalidaException si el
     *         préstamo no está en un estado evaluable (la API la traduce a HTTP 409)
     */
    @Transactional
    public EvaluacionCompletada evaluar(UUID prestamoId) {
        Prestamo prestamo = prestamos.buscarPorId(prestamoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Préstamo no encontrado: " + prestamoId));

        prestamo.cambiarEstado(new EstadoPrestamo.EnEvaluacion(LocalDateTime.now(), EVALUADOR));
        ResultadoEvaluacion resultado = motor.evaluar(prestamo.getCliente(), prestamo);

        EstadoPrestamo estadoFinal = resultado.decision().equals(MotorScoring.APROBADO)
                ? new EstadoPrestamo.Aprobado(LocalDateTime.now(), resultado.score())
                : new EstadoPrestamo.Rechazado(LocalDateTime.now(),
                        "Score %d por debajo del umbral %d"
                                .formatted(resultado.score(), MotorScoring.UMBRAL_APROBACION));
        prestamo.cambiarEstado(estadoFinal);

        Prestamo actualizado = prestamos.actualizarEstado(prestamoId, estadoFinal);
        log.info("evaluacion_completada prestamo={} score={} decision={}",
                prestamoId, resultado.score(), resultado.decision());
        return new EvaluacionCompletada(actualizado, resultado);
    }
}
