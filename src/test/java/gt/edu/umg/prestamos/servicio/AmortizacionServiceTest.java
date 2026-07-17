package gt.edu.umg.prestamos.servicio;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.cliente.ClienteIndividual;
import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.TipoEmpleo;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoAutomotriz;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoPersonal;
import gt.edu.umg.prestamos.persistencia.adaptador.PrestamoRepositorioJpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link AmortizacionService} y {@link CalculadoraInteresFactory}
 * (fábrica real, repositorio simulado).
 */
@ExtendWith(MockitoExtension.class)
class AmortizacionServiceTest {

    @Mock
    private PrestamoRepositorioJpa prestamos;

    private AmortizacionService servicio;

    @BeforeEach
    void configurar() {
        servicio = new AmortizacionService(prestamos, new CalculadoraInteresFactory());
    }

    private static Cliente cliente() {
        return new ClienteIndividual(UUID.randomUUID(), "María López", "254789",
                "maria@example.com", LocalDate.of(2020, 1, 15), HistorialCrediticio.BUENO,
                new BigDecimal("8000.00"), TipoEmpleo.FORMAL, 6);
    }

    private static Prestamo prestamoPersonal() {
        return new PrestamoPersonal(UUID.randomUUID(), cliente(),
                new BigDecimal("50000.00"), 60, new BigDecimal("0.12"),
                LocalDate.of(2026, 7, 1));
    }

    @Test
    void sinMetodoUsaElPorDefectoDelProducto() {
        // El préstamo automotriz amortiza por defecto con el método alemán.
        Prestamo automotriz = new PrestamoAutomotriz(UUID.randomUUID(), cliente(),
                new BigDecimal("80000.00"), 48, new BigDecimal("0.10"),
                LocalDate.of(2026, 7, 1), "Camión Hino 300", new BigDecimal("0.15"));
        when(prestamos.buscarPorId(automotriz.getId())).thenReturn(Optional.of(automotriz));

        var plan = servicio.generarPlan(automotriz.getId(), null);

        assertThat(plan.metodo()).isEqualTo("ALEMAN");
        assertThat(plan.cuotas()).hasSize(48);
    }

    @Test
    void conMetodoExplicitoUsaLaFabrica() {
        Prestamo personal = prestamoPersonal();
        when(prestamos.buscarPorId(personal.getId())).thenReturn(Optional.of(personal));

        var plan = servicio.generarPlan(personal.getId(), "aleman");

        assertThat(plan.metodo()).isEqualTo("ALEMAN");
        // Método alemán: capital constante = 50000 / 60 = 833.33.
        assertThat(plan.cuotas().getFirst().capital()).isEqualByComparingTo("833.33");
    }

    @Test
    void metodoNoSoportadoLanzaArgumentoInvalido() {
        Prestamo personal = prestamoPersonal();
        when(prestamos.buscarPorId(personal.getId())).thenReturn(Optional.of(personal));

        assertThatThrownBy(() -> servicio.generarPlan(personal.getId(), "BULLET"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BULLET");
    }

    @Test
    void prestamoInexistenteLanzaNoEncontrado() {
        UUID id = UUID.randomUUID();
        when(prestamos.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.generarPlan(id, null))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
