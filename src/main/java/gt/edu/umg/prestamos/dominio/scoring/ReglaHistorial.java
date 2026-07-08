package gt.edu.umg.prestamos.dominio.scoring;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;

/**
 * Evalúa el historial crediticio almacenado del cliente. Peso 25.
 *
 * <p>Brackets: BUENO → 100 · REGULAR → 60 · MALO → 0. El historial es un campo del
 * cliente; no se consulta ningún sistema externo (Sección 5 del CLAUDE.md).
 */
public final class ReglaHistorial implements ReglaScoring {

    private static final int PESO = 25;

    @Override
    public int evaluar(Cliente cliente, Prestamo prestamo) {
        return switch (cliente.getHistorial()) {
            case BUENO -> 100;
            case REGULAR -> 60;
            case MALO -> 0;
        };
    }

    @Override
    public int peso() {
        return PESO;
    }

    @Override
    public String descripcion() {
        return "Historial crediticio almacenado";
    }
}
