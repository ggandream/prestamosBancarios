package gt.edu.umg.prestamos.dominio.calculo;

import gt.edu.umg.prestamos.dominio.prestamo.Cuota;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests de las calculadoras contra valores calculados a mano.
 *
 * <p>Caso base común: monto = 12000, plazo = 12 meses, tasa anual = 0.12
 * (⇒ interés mensual i = 0.01). Primera cuota el 2025-02-01.
 */
class CalculadoraInteresTest {

    private static final BigDecimal MONTO = new BigDecimal("12000");
    private static final int PLAZO = 12;
    private static final BigDecimal TASA = new BigDecimal("0.12");
    private static final LocalDate PRIMERA = LocalDate.of(2025, 2, 1);

    private static void assertMonto(String esperado, BigDecimal actual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(actual),
                () -> "esperado " + esperado + " pero fue " + actual);
    }

    @Test
    @DisplayName("Método francés: cuota fija C = M*i/(1-(1+i)^-n) = 1066.19")
    void frances() {
        // 1.01^12 = 1.12682503 ; (1.01)^-12 = 0.88744923 ; 1 - 0.88744923 = 0.11255077
        // C = 12000 * 0.01 / 0.11255077 = 120 / 0.11255077 = 1066.19
        List<Cuota> plan = new MetodoFrances().calcular(MONTO, PLAZO, TASA, PRIMERA);

        assertEquals(PLAZO, plan.size());

        Cuota primera = plan.getFirst();
        assertEquals(1, primera.numero());
        assertEquals(PRIMERA, primera.fechaPago());
        assertMonto("1066.19", primera.total());
        assertMonto("120.00", primera.interes());      // 12000 * 0.01
        assertMonto("946.19", primera.capital());       // 1066.19 - 120.00

        // La segunda cuota vence un mes después.
        assertEquals(LocalDate.of(2025, 3, 1), plan.get(1).fechaPago());

        // La suma de capital amortizado debe igualar exactamente el monto prestado.
        BigDecimal capitalTotal = plan.stream().map(Cuota::capital)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertMonto("12000.00", capitalTotal);
    }

    @Test
    @DisplayName("Método alemán: capital constante 1000, interés sobre saldo decreciente")
    void aleman() {
        List<Cuota> plan = new MetodoAleman().calcular(MONTO, PLAZO, TASA, PRIMERA);

        assertEquals(PLAZO, plan.size());

        Cuota primera = plan.getFirst();
        assertMonto("1000.00", primera.capital());      // 12000 / 12
        assertMonto("120.00", primera.interes());        // 12000 * 0.01
        assertMonto("1120.00", primera.total());

        Cuota segunda = plan.get(1);
        assertMonto("1000.00", segunda.capital());
        assertMonto("110.00", segunda.interes());        // 11000 * 0.01
        assertMonto("1110.00", segunda.total());

        Cuota ultima = plan.getLast();
        assertMonto("1000.00", ultima.capital());        // salda el remanente
        assertMonto("10.00", ultima.interes());          // 1000 * 0.01
        assertMonto("1010.00", ultima.total());

        BigDecimal capitalTotal = plan.stream().map(Cuota::capital)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertMonto("12000.00", capitalTotal);
    }

    @Test
    @DisplayName("El alemán paga menos interés total que el francés (amortiza capital antes)")
    void alemanPagaMenosInteres() {
        BigDecimal interesFrances = new MetodoFrances().calcular(MONTO, PLAZO, TASA, PRIMERA)
                .stream().map(Cuota::interes).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal interesAleman = new MetodoAleman().calcular(MONTO, PLAZO, TASA, PRIMERA)
                .stream().map(Cuota::interes).reduce(BigDecimal.ZERO, BigDecimal::add);

        org.junit.jupiter.api.Assertions.assertTrue(interesAleman.compareTo(interesFrances) < 0,
                "alemán=" + interesAleman + " debería ser < francés=" + interesFrances);
    }
}
