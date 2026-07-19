package gt.edu.umg.prestamos.dominio.scoring;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.cliente.ClienteIndividual;
import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.TipoEmpleo;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoPersonal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests de {@link ReglaHistorial}, incluido el enriquecimiento con préstamos previos. */
class ReglaHistorialTest {

    private static Cliente cliente(HistorialCrediticio historial) {
        return new ClienteIndividual(UUID.randomUUID(), "Ana", "1234", "ana@mail.com",
                LocalDate.of(2020, 1, 1), historial,
                new BigDecimal("10000"), TipoEmpleo.FORMAL, 3);
    }

    private static Prestamo prestamoDe(Cliente cliente) {
        return new PrestamoPersonal(UUID.randomUUID(), cliente, new BigDecimal("12000"),
                12, new BigDecimal("0.12"), LocalDate.of(2026, 1, 1));
    }

    private static Prestamo previoEnEstado(Cliente cliente, EstadoPrestamo estado) {
        Prestamo previo = prestamoDe(cliente);
        previo.restaurarEstado(estado);
        return previo;
    }

    @Test
    @DisplayName("Sin préstamos previos: puntaje base por historial almacenado")
    void puntajeBasePorHistorialAlmacenado() {
        ReglaHistorial regla = new ReglaHistorial();
        assertEquals(100, regla.evaluar(cliente(HistorialCrediticio.BUENO),
                prestamoDe(cliente(HistorialCrediticio.BUENO))));
        assertEquals(60, regla.evaluar(cliente(HistorialCrediticio.REGULAR),
                prestamoDe(cliente(HistorialCrediticio.REGULAR))));
        assertEquals(0, regla.evaluar(cliente(HistorialCrediticio.MALO),
                prestamoDe(cliente(HistorialCrediticio.MALO))));
    }

    @Test
    @DisplayName("Un préstamo previo en mora anula el puntaje aunque el historial sea BUENO")
    void previoEnMoraAnulaPuntaje() {
        Cliente cliente = cliente(HistorialCrediticio.BUENO);
        Prestamo moroso = previoEnEstado(cliente,
                new EstadoPrestamo.EnMora(45, new BigDecimal("2500.00")));
        ReglaHistorial regla = new ReglaHistorial(clienteId -> List.of(moroso));

        assertEquals(0, regla.evaluar(cliente, prestamoDe(cliente)));
    }

    @Test
    @DisplayName("Un préstamo previo pagado da bono de 20 con tope en 100")
    void previoPagadoDaBonoConTope() {
        Cliente regular = cliente(HistorialCrediticio.REGULAR);
        Prestamo pagado = previoEnEstado(regular,
                new EstadoPrestamo.Pagado(LocalDateTime.of(2025, 6, 1, 10, 0)));
        ReglaHistorial regla = new ReglaHistorial(clienteId -> List.of(pagado));

        assertEquals(80, regla.evaluar(regular, prestamoDe(regular)));

        Cliente bueno = cliente(HistorialCrediticio.BUENO);
        Prestamo pagadoDeBueno = previoEnEstado(bueno,
                new EstadoPrestamo.Pagado(LocalDateTime.of(2025, 6, 1, 10, 0)));
        ReglaHistorial reglaBueno = new ReglaHistorial(clienteId -> List.of(pagadoDeBueno));

        assertEquals(100, reglaBueno.evaluar(bueno, prestamoDe(bueno)));
    }

    @Test
    @DisplayName("El préstamo en evaluación se excluye de los previos")
    void excluyeElPrestamoEvaluado() {
        Cliente cliente = cliente(HistorialCrediticio.BUENO);
        Prestamo actual = prestamoDe(cliente);
        // La consulta devuelve el mismo préstamo que se está evaluando (ya persistido).
        ReglaHistorial regla = new ReglaHistorial(clienteId -> List.of(actual));

        assertEquals(100, regla.evaluar(cliente, actual));
    }
}
