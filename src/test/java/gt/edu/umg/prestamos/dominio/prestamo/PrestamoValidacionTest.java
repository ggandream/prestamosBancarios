package gt.edu.umg.prestamos.dominio.prestamo;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.cliente.ClienteIndividual;
import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.TipoEmpleo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrestamoValidacionTest {

    private static final LocalDate FECHA = LocalDate.of(2025, 1, 1);

    private static Cliente cliente() {
        return new ClienteIndividual(UUID.randomUUID(), "Ana", "1234", "ana@mail.com",
                LocalDate.of(2020, 1, 1), HistorialCrediticio.BUENO,
                new BigDecimal("10000"), TipoEmpleo.FORMAL, 3);
    }

    private static PrestamoPersonal personal(BigDecimal monto, int plazo, BigDecimal tasa) {
        return new PrestamoPersonal(UUID.randomUUID(), cliente(), monto, plazo, tasa, FECHA);
    }

    @Test
    void montoNoPositivoRechazado() {
        assertThrows(IllegalArgumentException.class,
                () -> personal(BigDecimal.ZERO, 12, new BigDecimal("0.12")));
        assertThrows(IllegalArgumentException.class,
                () -> personal(new BigDecimal("-100"), 12, new BigDecimal("0.12")));
    }

    @Test
    void plazoFueraDeRangoRechazado() {
        assertThrows(IllegalArgumentException.class,
                () -> personal(new BigDecimal("12000"), 5, new BigDecimal("0.12")));   // < 6
        assertThrows(IllegalArgumentException.class,
                () -> personal(new BigDecimal("12000"), 361, new BigDecimal("0.12"))); // > 360
    }

    @Test
    void plazoLimiteAceptado() {
        assertDoesNotThrow(() -> personal(new BigDecimal("12000"), 6, new BigDecimal("0.12")));
        assertDoesNotThrow(() -> personal(new BigDecimal("12000"), 360, new BigDecimal("0.12")));
    }

    @Test
    void tasaNoPositivaRechazada() {
        assertThrows(IllegalArgumentException.class,
                () -> personal(new BigDecimal("12000"), 12, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Hipotecario: monto no puede superar el 80% del avalúo")
    void hipotecarioSuperaLtvRechazado() {
        // avalúo 100000 -> tope 80000
        assertThrows(IllegalArgumentException.class, () -> new PrestamoHipotecario(
                UUID.randomUUID(), cliente(), new BigDecimal("90000"), 120,
                new BigDecimal("0.10"), FECHA, "Casa zona 1", new BigDecimal("100000")));

        assertDoesNotThrow(() -> new PrestamoHipotecario(
                UUID.randomUUID(), cliente(), new BigDecimal("80000"), 120,
                new BigDecimal("0.10"), FECHA, "Casa zona 1", new BigDecimal("100000")));
    }

    @Test
    @DisplayName("Automotriz usa método alemán: la primera cuota tiene mayor total que la última")
    void automotrizUsaAleman() {
        PrestamoAutomotriz auto = new PrestamoAutomotriz(UUID.randomUUID(), cliente(),
                new BigDecimal("12000"), 12, new BigDecimal("0.12"), FECHA,
                "Toyota Yaris", new BigDecimal("0.15"));

        var plan = auto.generarPlanPagos();
        org.junit.jupiter.api.Assertions.assertTrue(
                plan.getFirst().total().compareTo(plan.getLast().total()) > 0,
                "en amortización alemana la cuota decrece");
    }
}
