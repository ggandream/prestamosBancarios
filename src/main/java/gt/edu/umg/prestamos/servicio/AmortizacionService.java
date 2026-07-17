package gt.edu.umg.prestamos.servicio;

import gt.edu.umg.prestamos.dominio.calculo.CalculadoraInteres;
import gt.edu.umg.prestamos.dominio.prestamo.Cuota;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;
import gt.edu.umg.prestamos.persistencia.adaptador.PrestamoRepositorioJpa;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de aplicación que genera el plan de amortización de un préstamo. La
 * estrategia de cálculo se selecciona con {@link CalculadoraInteresFactory} (patrón
 * Factory); si no se pide un método explícito se usa la calculadora por defecto del
 * producto (Strategy del dominio).
 */
@Service
public class AmortizacionService {

    private final PrestamoRepositorioJpa prestamos;
    private final CalculadoraInteresFactory factory;

    public AmortizacionService(PrestamoRepositorioJpa prestamos, CalculadoraInteresFactory factory) {
        this.prestamos = prestamos;
        this.factory = factory;
    }

    /** Plan generado junto con el préstamo y el nombre del método aplicado. */
    public record PlanAmortizacion(Prestamo prestamo, String metodo, List<Cuota> cuotas) {}

    /**
     * Genera el plan de pagos de un préstamo.
     *
     * @param prestamoId identificador del préstamo
     * @param metodo     {@code FRANCES}, {@code ALEMAN} o {@code null}/vacío para usar
     *                   el método por defecto del producto
     * @throws RecursoNoEncontradoException si el préstamo no existe
     * @throws IllegalArgumentException si el método pedido no es soportado
     */
    public PlanAmortizacion generarPlan(UUID prestamoId, String metodo) {
        Prestamo prestamo = prestamos.buscarPorId(prestamoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Préstamo no encontrado: " + prestamoId));
        CalculadoraInteres calculadora = (metodo == null || metodo.isBlank())
                ? prestamo.calculadoraPorDefecto()
                : factory.crear(metodo);
        return new PlanAmortizacion(prestamo, calculadora.nombre(),
                prestamo.generarPlanPagos(calculadora));
    }
}
