package gt.edu.umg.prestamos.config;

import gt.edu.umg.prestamos.dominio.scoring.MotorScoring;
import gt.edu.umg.prestamos.dominio.scoring.ReglaCapacidadPago;
import gt.edu.umg.prestamos.dominio.scoring.ReglaEdad;
import gt.edu.umg.prestamos.dominio.scoring.ReglaHistorial;
import gt.edu.umg.prestamos.dominio.scoring.ReglaIngreso;
import gt.edu.umg.prestamos.dominio.scoring.ReglaScoring;
import gt.edu.umg.prestamos.persistencia.adaptador.PrestamoRepositorioJpa;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Registra las reglas de scoring del dominio como beans y arma el {@link MotorScoring}
 * inyectándolas como {@code List<ReglaScoring>}.
 *
 * <p>Esto materializa Strategy + Open/Closed: el dominio queda libre de anotaciones
 * (regla de arquitectura de la Sección 3.1 del CLAUDE.md) y el motor no se modifica
 * para combinar reglas — aunque el set de 4 reglas está cerrado por diseño.
 */
@Configuration
public class ScoringConfig {

    @Bean
    public ReglaScoring reglaCapacidadPago() {
        return new ReglaCapacidadPago();
    }

    @Bean
    public ReglaScoring reglaIngreso() {
        return new ReglaIngreso();
    }

    /**
     * Regla de historial enriquecida (Fase 4): recibe la consulta de préstamos previos
     * ya resuelta contra el repositorio. El dominio solo ve la interfaz funcional
     * {@code ConsultaPrestamosPrevios}; la persistencia nunca entra al dominio.
     */
    @Bean
    public ReglaScoring reglaHistorial(PrestamoRepositorioJpa prestamos) {
        return new ReglaHistorial(prestamos::buscarPorCliente);
    }

    @Bean
    public ReglaScoring reglaEdad() {
        return new ReglaEdad();
    }

    @Bean
    public MotorScoring motorScoring(List<ReglaScoring> reglas) {
        return new MotorScoring(reglas);
    }
}
