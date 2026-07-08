package gt.edu.umg.prestamos.dominio.scoring;

import java.util.List;

/**
 * Resultado de una evaluación de scoring.
 *
 * @param score    puntaje ponderado final, entero en [0, 100]
 * @param decision "APROBADO" o "RECHAZADO" (Sección 5 del CLAUDE.md: dos resultados)
 * @param detalle  desglose legible del aporte de cada regla
 */
public record ResultadoEvaluacion(int score, String decision, List<String> detalle) {

    public ResultadoEvaluacion {
        detalle = List.copyOf(detalle);
    }
}
