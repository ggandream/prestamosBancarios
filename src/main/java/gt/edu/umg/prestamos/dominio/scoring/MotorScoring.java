package gt.edu.umg.prestamos.dominio.scoring;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Motor de scoring determinista. Recibe las reglas por constructor (patrón Strategy +
 * principio OCP: se pueden combinar reglas sin modificar el motor) y produce el
 * puntaje como <strong>promedio ponderado</strong> de los aportes de cada regla,
 * usando el peso fijo que cada regla declara.
 *
 * <p>Decisión binaria (Sección 5 del CLAUDE.md): {@code score >= 60} → APROBADO,
 * {@code score < 60} → RECHAZADO.
 */
public final class MotorScoring {

    /** Umbral de aprobación. */
    public static final int UMBRAL_APROBACION = 60;
    public static final String APROBADO = "APROBADO";
    public static final String RECHAZADO = "RECHAZADO";

    private final List<ReglaScoring> reglas;

    public MotorScoring(List<ReglaScoring> reglas) {
        Objects.requireNonNull(reglas, "reglas no puede ser null");
        if (reglas.isEmpty()) {
            throw new IllegalArgumentException("se requiere al menos una regla de scoring");
        }
        this.reglas = List.copyOf(reglas);
    }

    public ResultadoEvaluacion evaluar(Cliente cliente, Prestamo prestamo) {
        BigDecimal sumaPonderada = BigDecimal.ZERO;
        int sumaPesos = 0;
        List<String> detalle = new ArrayList<>(reglas.size());

        for (ReglaScoring regla : reglas) {
            int puntaje = regla.evaluar(cliente, prestamo);
            int peso = regla.peso();
            sumaPonderada = sumaPonderada.add(BigDecimal.valueOf((long) puntaje * peso));
            sumaPesos += peso;
            detalle.add("%s => %d (peso %d)".formatted(regla.descripcion(), puntaje, peso));
        }

        int score = sumaPonderada
                .divide(BigDecimal.valueOf(sumaPesos), MathContext.DECIMAL64)
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
        String decision = score >= UMBRAL_APROBACION ? APROBADO : RECHAZADO;

        return new ResultadoEvaluacion(score, decision, detalle);
    }
}
