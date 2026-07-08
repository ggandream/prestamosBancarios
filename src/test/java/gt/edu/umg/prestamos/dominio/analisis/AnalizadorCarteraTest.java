package gt.edu.umg.prestamos.dominio.analisis;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.cliente.ClienteIndividual;
import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.TipoEmpleo;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Aprobado;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Desembolsado;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.EnEvaluacion;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.EnMora;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoAutomotriz;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoHipotecario;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoPersonal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalizadorCarteraTest {

    private static final LocalDate FECHA = LocalDate.of(2025, 1, 1);
    private static final LocalDateTime AHORA = LocalDateTime.of(2025, 1, 1, 10, 0);

    private static Cliente cliente() {
        return new ClienteIndividual(UUID.randomUUID(), "Ana", "1234", "ana@mail.com",
                LocalDate.of(2020, 1, 1), HistorialCrediticio.BUENO,
                new BigDecimal("10000"), TipoEmpleo.FORMAL, 3);
    }

    private static void aprobarYDesembolsar(Prestamo p) {
        p.cambiarEstado(new EnEvaluacion(AHORA, "sistema"));
        p.cambiarEstado(new Aprobado(AHORA, 85));
        p.cambiarEstado(new Desembolsado(AHORA, p.getMonto()));
    }

    private static void assertMonto(String esperado, BigDecimal actual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(actual),
                () -> "esperado " + esperado + " pero fue " + actual);
    }

    @Test
    @DisplayName("Resumen: totales, agrupaciones por tipo/riesgo e índice de mora")
    void resumenCompleto() {
        Prestamo desembolsado = new PrestamoPersonal(UUID.randomUUID(), cliente(),
                new BigDecimal("10000"), 12, new BigDecimal("0.12"), FECHA);
        aprobarYDesembolsar(desembolsado);

        Prestamo enMora = new PrestamoHipotecario(UUID.randomUUID(), cliente(),
                new BigDecimal("80000"), 120, new BigDecimal("0.10"), FECHA, "Casa", new BigDecimal("100000"));
        aprobarYDesembolsar(enMora);
        enMora.cambiarEstado(new EnMora(30, new BigDecimal("1000")));

        Prestamo aprobado = new PrestamoAutomotriz(UUID.randomUUID(), cliente(),
                new BigDecimal("12000"), 12, new BigDecimal("0.12"), FECHA, "Yaris", new BigDecimal("0.15"));
        aprobado.cambiarEstado(new EnEvaluacion(AHORA, "sistema"));
        aprobado.cambiarEstado(new Aprobado(AHORA, 80));

        Prestamo borrador = new PrestamoPersonal(UUID.randomUUID(), cliente(),
                new BigDecimal("5000"), 12, new BigDecimal("0.12"), FECHA);

        ResumenCartera r = new AnalizadorCartera()
                .analizar(List.of(desembolsado, enMora, aprobado, borrador));

        assertEquals(4, r.totalPrestamos());
        assertMonto("107000.00", r.montoTotal());
        assertMonto("26750.00", r.montoPromedio());
        assertMonto("5000.00", r.montoMinimo());
        assertMonto("80000.00", r.montoMaximo());

        assertEquals(2L, r.conteoPorTipo().get("PrestamoPersonal"));
        assertEquals(1L, r.conteoPorTipo().get("PrestamoHipotecario"));
        assertEquals(1L, r.conteoPorTipo().get("PrestamoAutomotriz"));

        assertEquals(1L, r.conteoPorRiesgo().get("MEDIO"));       // desembolsado
        assertEquals(1L, r.conteoPorRiesgo().get("ALTO"));        // en mora
        assertEquals(1L, r.conteoPorRiesgo().get("BAJO"));        // aprobado
        assertEquals(1L, r.conteoPorRiesgo().get("SIN_RIESGO"));  // borrador

        // índice de mora = 80000 / (10000 + 80000) = 0.8889
        assertMonto("0.8889", r.indiceMora());
    }

    @Test
    @DisplayName("Cartera vacía produce un resumen en ceros sin fallar")
    void carteraVacia() {
        ResumenCartera r = new AnalizadorCartera().analizar(List.of());
        assertEquals(0, r.totalPrestamos());
        assertMonto("0", r.montoTotal());
        assertMonto("0", r.indiceMora());
    }
}
