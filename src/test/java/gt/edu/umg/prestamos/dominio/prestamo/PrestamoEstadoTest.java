package gt.edu.umg.prestamos.dominio.prestamo;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.cliente.ClienteIndividual;
import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.TipoEmpleo;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Aprobado;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Desembolsado;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.EnEvaluacion;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.EnMora;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Pagado;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Rechazado;
import gt.edu.umg.prestamos.dominio.estado.TransicionInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrestamoEstadoTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2025, 1, 1, 10, 0);

    private static Cliente cliente() {
        return new ClienteIndividual(UUID.randomUUID(), "Ana", "1234", "ana@mail.com",
                LocalDate.of(2020, 1, 1), HistorialCrediticio.BUENO,
                new BigDecimal("10000"), TipoEmpleo.FORMAL, 3);
    }

    private static Prestamo prestamo() {
        return new PrestamoPersonal(UUID.randomUUID(), cliente(), new BigDecimal("12000"),
                12, new BigDecimal("0.12"), LocalDate.of(2025, 1, 1));
    }

    @Test
    @DisplayName("Flujo feliz completo: Borrador→EnEvaluacion→Aprobado→Desembolsado→Pagado")
    void flujoFeliz() {
        Prestamo p = prestamo();
        assertInstanceOf(gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Borrador.class, p.getEstado());
        p.cambiarEstado(new EnEvaluacion(AHORA, "sistema"));
        p.cambiarEstado(new Aprobado(AHORA, 85));
        p.cambiarEstado(new Desembolsado(AHORA, new BigDecimal("12000")));
        p.cambiarEstado(new Pagado(AHORA));
        assertInstanceOf(Pagado.class, p.getEstado());
    }

    @Test
    @DisplayName("Bifurcación a rechazo: EnEvaluacion→Rechazado")
    void rechazo() {
        Prestamo p = prestamo();
        p.cambiarEstado(new EnEvaluacion(AHORA, "sistema"));
        assertDoesNotThrow(() -> p.cambiarEstado(new Rechazado(AHORA, "score bajo")));
    }

    @Test
    @DisplayName("Mora y recuperación: Desembolsado→EnMora→Pagado")
    void moraYRecuperacion() {
        Prestamo p = prestamo();
        p.cambiarEstado(new EnEvaluacion(AHORA, "sistema"));
        p.cambiarEstado(new Aprobado(AHORA, 85));
        p.cambiarEstado(new Desembolsado(AHORA, new BigDecimal("12000")));
        p.cambiarEstado(new EnMora(30, new BigDecimal("1066.19")));
        assertDoesNotThrow(() -> p.cambiarEstado(new Pagado(AHORA)));
    }

    @Test
    @DisplayName("Transición inválida desde Borrador salta a Aprobado")
    void borradorNoSaltaAAprobado() {
        Prestamo p = prestamo();
        assertThrows(TransicionInvalidaException.class, () -> p.cambiarEstado(new Aprobado(AHORA, 90)));
    }

    @Test
    @DisplayName("Aprobado no puede pasar directo a Pagado")
    void aprobadoNoPasaAPagado() {
        Prestamo p = prestamo();
        p.cambiarEstado(new EnEvaluacion(AHORA, "sistema"));
        p.cambiarEstado(new Aprobado(AHORA, 85));
        assertThrows(TransicionInvalidaException.class, () -> p.cambiarEstado(new Pagado(AHORA)));
    }

    @Test
    @DisplayName("Estados terminales (Rechazado, Pagado) no admiten más transiciones")
    void terminalesNoTransicionan() {
        Prestamo rechazado = prestamo();
        rechazado.cambiarEstado(new EnEvaluacion(AHORA, "sistema"));
        rechazado.cambiarEstado(new Rechazado(AHORA, "score bajo"));
        assertThrows(TransicionInvalidaException.class,
                () -> rechazado.cambiarEstado(new EnEvaluacion(AHORA, "otro")));

        Prestamo pagado = prestamo();
        pagado.cambiarEstado(new EnEvaluacion(AHORA, "sistema"));
        pagado.cambiarEstado(new Aprobado(AHORA, 85));
        pagado.cambiarEstado(new Desembolsado(AHORA, new BigDecimal("12000")));
        pagado.cambiarEstado(new Pagado(AHORA));
        assertThrows(TransicionInvalidaException.class,
                () -> pagado.cambiarEstado(new EnMora(1, BigDecimal.TEN)));
    }

    @Test
    @DisplayName("descripcionEstado cubre los 7 estados (switch exhaustivo)")
    void descripcionTodosLosEstados() {
        Prestamo p = prestamo();
        assertTrue(p.descripcionEstado().contains("Borrador"));

        p.cambiarEstado(new EnEvaluacion(AHORA, "sistema"));
        assertTrue(p.descripcionEstado().contains("evaluación"));

        p.cambiarEstado(new Aprobado(AHORA, 85));
        assertTrue(p.descripcionEstado().contains("85"));

        p.cambiarEstado(new Desembolsado(AHORA, new BigDecimal("12000")));
        assertTrue(p.descripcionEstado().contains("Desembolsado"));

        p.cambiarEstado(new EnMora(30, new BigDecimal("1066.19")));
        assertTrue(p.descripcionEstado().contains("mora"));

        p.cambiarEstado(new Pagado(AHORA));
        assertTrue(p.descripcionEstado().contains("Pagado"));

        // Rechazado en un préstamo aparte (rama alterna del flujo).
        Prestamo r = prestamo();
        r.cambiarEstado(new EnEvaluacion(AHORA, "sistema"));
        r.cambiarEstado(new Rechazado(AHORA, "motivo X"));
        assertTrue(r.descripcionEstado().contains("Rechazado"));
    }
}
