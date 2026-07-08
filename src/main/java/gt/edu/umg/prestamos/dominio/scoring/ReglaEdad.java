package gt.edu.umg.prestamos.dominio.scoring;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;

/**
 * Evalúa la antigüedad del vínculo del cliente (antigüedad laboral en el individual,
 * antigüedad del NIT en el empresarial), como proxy de estabilidad. Peso 10.
 *
 * <p>El modelo del Entregable 1 no incluye fecha de nacimiento, por lo que esta regla
 * ({@code ReglaEdad} en el conjunto cerrado de la Sección 4) se implementa sobre la
 * antigüedad expuesta por {@link Cliente#getAntiguedadAnios()}.
 *
 * <p>Brackets: &ge; 5 años → 100 · &ge; 2 → 70 · &ge; 1 → 40 · resto → 0.
 */
public final class ReglaEdad implements ReglaScoring {

    private static final int PESO = 10;

    @Override
    public int evaluar(Cliente cliente, Prestamo prestamo) {
        int anios = cliente.getAntiguedadAnios();
        if (anios >= 5) return 100;
        if (anios >= 2) return 70;
        if (anios >= 1) return 40;
        return 0;
    }

    @Override
    public int peso() {
        return PESO;
    }

    @Override
    public String descripcion() {
        return "Antigüedad del vínculo (laboral / NIT)";
    }
}
