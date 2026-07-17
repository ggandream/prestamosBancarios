package gt.edu.umg.prestamos.dominio.scoring;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Evalúa el historial crediticio del cliente. Peso 25.
 *
 * <p>Puntaje base por el historial almacenado: BUENO → 100 · REGULAR → 60 · MALO → 0.
 * El historial es un campo del cliente; no se consulta ningún sistema externo
 * (Sección 5 del CLAUDE.md).
 *
 * <p>Enriquecimiento de la Fase 4: la regla también considera los préstamos previos
 * del cliente, recibidos a través de {@link ConsultaPrestamosPrevios}. La consulta la
 * resuelve la capa de servicio/configuración (que la implementa con el repositorio);
 * el dominio solo ve esta interfaz funcional de Java puro y nunca conoce la
 * persistencia. Ajustes deterministas:
 * <ul>
 *   <li>algún préstamo previo en mora → 0 (independiente del historial almacenado)</li>
 *   <li>algún préstamo previo pagado → base + {@value #BONO_PAGADO} (tope 100)</li>
 * </ul>
 */
public final class ReglaHistorial implements ReglaScoring {

    /** Fuente de préstamos previos de un cliente; la provee la capa de servicio. */
    @FunctionalInterface
    public interface ConsultaPrestamosPrevios {
        List<Prestamo> prestamosDe(UUID clienteId);
    }

    private static final int PESO = 25;
    /** Bono por buen comportamiento comprobado (al menos un préstamo pagado). */
    private static final int BONO_PAGADO = 20;

    private final ConsultaPrestamosPrevios consultaPrevios;

    /** Variante sin préstamos previos (Entregable 1 / tests de dominio). */
    public ReglaHistorial() {
        this(clienteId -> List.of());
    }

    public ReglaHistorial(ConsultaPrestamosPrevios consultaPrevios) {
        this.consultaPrevios = Objects.requireNonNull(
                consultaPrevios, "consultaPrevios no puede ser null");
    }

    @Override
    public int evaluar(Cliente cliente, Prestamo prestamo) {
        List<Prestamo> previos = consultaPrevios.prestamosDe(cliente.getId()).stream()
                .filter(p -> !p.getId().equals(prestamo.getId()))
                .toList();

        boolean tieneMora = previos.stream()
                .anyMatch(p -> p.getEstado() instanceof EstadoPrestamo.EnMora);
        if (tieneMora) {
            return 0;
        }

        int base = switch (cliente.getHistorial()) {
            case BUENO -> 100;
            case REGULAR -> 60;
            case MALO -> 0;
        };

        boolean tienePagado = previos.stream()
                .anyMatch(p -> p.getEstado() instanceof EstadoPrestamo.Pagado);
        return tienePagado ? Math.min(100, base + BONO_PAGADO) : base;
    }

    @Override
    public int peso() {
        return PESO;
    }

    @Override
    public String descripcion() {
        return "Historial crediticio almacenado + préstamos previos";
    }
}
