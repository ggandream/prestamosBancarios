package gt.edu.umg.prestamos.dominio.cliente;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClienteTest {

    private static final UUID ID = UUID.randomUUID();
    private static final LocalDate REGISTRO = LocalDate.of(2020, 1, 1);

    private static void assertMonto(String esperado, BigDecimal actual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(actual),
                () -> "esperado " + esperado + " pero fue " + actual);
    }

    private static ClienteIndividual individual(BigDecimal salario, TipoEmpleo empleo) {
        return new ClienteIndividual(ID, "Ana", "1234", "ana@mail.com", REGISTRO,
                HistorialCrediticio.BUENO, salario, empleo, 3);
    }

    private static ClienteEmpresarial empresarial(BigDecimal facturacion, SectorIndustria sector) {
        return new ClienteEmpresarial(ID, "ACME", "1234", "acme@mail.com", REGISTRO,
                HistorialCrediticio.REGULAR, facturacion, "NIT-9", sector, 4);
    }

    @Nested
    @DisplayName("Capacidad de pago por tipo de cliente")
    class Capacidad {

        @Test
        @DisplayName("Individual FORMAL: 35% del salario, factor 1.00")
        void individualFormal() {
            assertMonto("3500.00", individual(new BigDecimal("10000"), TipoEmpleo.FORMAL).getCapacidadPago());
        }

        @Test
        @DisplayName("Individual INFORMAL: 35% del salario, factor 0.75")
        void individualInformal() {
            assertMonto("2625.00", individual(new BigDecimal("10000"), TipoEmpleo.INFORMAL).getCapacidadPago());
        }

        @Test
        @DisplayName("Individual INDEPENDIENTE: 35% del salario, factor 0.90")
        void individualIndependiente() {
            assertMonto("3150.00", individual(new BigDecimal("10000"), TipoEmpleo.INDEPENDIENTE).getCapacidadPago());
        }

        @Test
        @DisplayName("Empresarial COMERCIO: 10% de facturación mensual, factor 1.00")
        void empresarialComercio() {
            // facturación anual 120000 -> mensual 10000 -> 10% = 1000 -> factor 1.00
            assertMonto("1000.00", empresarial(new BigDecimal("120000"), SectorIndustria.COMERCIO).getCapacidadPago());
        }

        @Test
        @DisplayName("Empresarial AGRICOLA: 10% de facturación mensual, factor 0.85")
        void empresarialAgricola() {
            assertMonto("850.00", empresarial(new BigDecimal("120000"), SectorIndustria.AGRICOLA).getCapacidadPago());
        }

        @Test
        @DisplayName("Ingreso mensual: salario (individual) y facturación/12 (empresarial)")
        void ingresoMensual() {
            assertMonto("10000.00", individual(new BigDecimal("10000"), TipoEmpleo.FORMAL).getIngresoMensual());
            assertMonto("10000.00", empresarial(new BigDecimal("120000"), SectorIndustria.COMERCIO).getIngresoMensual());
        }
    }

    @Nested
    @DisplayName("Validaciones de constructor (datos incoherentes rechazados)")
    class Validaciones {

        @Test
        void nombreVacioRechazado() {
            assertThrows(IllegalArgumentException.class, () -> new ClienteIndividual(
                    ID, "  ", "1234", "ana@mail.com", REGISTRO, HistorialCrediticio.BUENO,
                    new BigDecimal("1000"), TipoEmpleo.FORMAL, 1));
        }

        @Test
        void documentoVacioRechazado() {
            assertThrows(IllegalArgumentException.class, () -> new ClienteIndividual(
                    ID, "Ana", "", "ana@mail.com", REGISTRO, HistorialCrediticio.BUENO,
                    new BigDecimal("1000"), TipoEmpleo.FORMAL, 1));
        }

        @Test
        void emailInvalidoRechazado() {
            assertThrows(IllegalArgumentException.class, () -> new ClienteIndividual(
                    ID, "Ana", "1234", "correo-invalido", REGISTRO, HistorialCrediticio.BUENO,
                    new BigDecimal("1000"), TipoEmpleo.FORMAL, 1));
        }

        @Test
        void salarioNoPositivoRechazado() {
            assertThrows(IllegalArgumentException.class, () -> new ClienteIndividual(
                    ID, "Ana", "1234", "ana@mail.com", REGISTRO, HistorialCrediticio.BUENO,
                    BigDecimal.ZERO, TipoEmpleo.FORMAL, 1));
        }

        @Test
        void facturacionNegativaRechazada() {
            assertThrows(IllegalArgumentException.class, () -> new ClienteEmpresarial(
                    ID, "ACME", "1234", "acme@mail.com", REGISTRO, HistorialCrediticio.MALO,
                    new BigDecimal("-1"), "NIT-9", SectorIndustria.SERVICIOS, 2));
        }

        @Test
        void antiguedadNegativaRechazada() {
            assertThrows(IllegalArgumentException.class, () -> new ClienteIndividual(
                    ID, "Ana", "1234", "ana@mail.com", REGISTRO, HistorialCrediticio.BUENO,
                    new BigDecimal("1000"), TipoEmpleo.FORMAL, -1));
        }
    }

    @Test
    @DisplayName("Score base determinista según antigüedad como cliente")
    void scoreBase() {
        ClienteIndividual c = individual(new BigDecimal("10000"), TipoEmpleo.FORMAL); // registro 2020-01-01
        assertEquals(100, c.calcularScoreBase(LocalDate.of(2025, 6, 1))); // >= 5 años
        assertEquals(80, c.calcularScoreBase(LocalDate.of(2023, 6, 1)));  // >= 3 años
        assertEquals(60, c.calcularScoreBase(LocalDate.of(2021, 6, 1)));  // >= 1 año
        assertEquals(40, c.calcularScoreBase(LocalDate.of(2020, 6, 1)));  // < 1 año
    }
}
