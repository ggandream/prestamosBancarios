package gt.edu.umg.prestamos.servicio;

import gt.edu.umg.prestamos.dominio.calculo.CalculadoraInteres;
import gt.edu.umg.prestamos.dominio.calculo.MetodoAleman;
import gt.edu.umg.prestamos.dominio.calculo.MetodoFrances;
import org.springframework.stereotype.Component;

/**
 * Fábrica de estrategias de amortización (patrón Factory): traduce el nombre del
 * método pedido por la API al {@link CalculadoraInteres} concreto del dominio, de modo
 * que ni el controlador ni el servicio conocen las clases concretas.
 */
@Component
public class CalculadoraInteresFactory {

    /**
     * Crea la calculadora correspondiente al método pedido.
     *
     * @param metodo {@code FRANCES} o {@code ALEMAN} (insensible a mayúsculas)
     * @throws IllegalArgumentException si el método no es uno de los dos soportados
     */
    public CalculadoraInteres crear(String metodo) {
        return switch (metodo.trim().toUpperCase()) {
            case "FRANCES" -> new MetodoFrances();
            case "ALEMAN" -> new MetodoAleman();
            default -> throw new IllegalArgumentException(
                    "Método de amortización no soportado: " + metodo + " (use FRANCES o ALEMAN)");
        };
    }
}
