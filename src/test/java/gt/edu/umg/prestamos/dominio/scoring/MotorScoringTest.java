package gt.edu.umg.prestamos.dominio.scoring;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.cliente.ClienteIndividual;
import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.TipoEmpleo;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoPersonal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MotorScoringTest {

    /** Regla simulada con puntaje y peso fijos, para aislar la lógica del motor. */
    private record ReglaFalsa(int puntaje, int pesoRegla) implements ReglaScoring {
        @Override public int evaluar(Cliente c, Prestamo p) { return puntaje; }
        @Override public int peso() { return pesoRegla; }
        @Override public String descripcion() { return "falsa(" + puntaje + "," + pesoRegla + ")"; }
    }

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
    @DisplayName("Todas las reglas al máximo → score 100 → APROBADO")
    void apruebaConScoreAlto() {
        MotorScoring motor = new MotorScoring(List.of(
                new ReglaFalsa(100, 40), new ReglaFalsa(100, 25),
                new ReglaFalsa(100, 25), new ReglaFalsa(100, 10)));

        ResultadoEvaluacion r = motor.evaluar(cliente(), prestamo());
        assertEquals(100, r.score());
        assertEquals(MotorScoring.APROBADO, r.decision());
        assertEquals(4, r.detalle().size());
    }

    @Test
    @DisplayName("Todas las reglas en cero → score 0 → RECHAZADO")
    void rechazaConScoreCero() {
        MotorScoring motor = new MotorScoring(List.of(
                new ReglaFalsa(0, 40), new ReglaFalsa(0, 25),
                new ReglaFalsa(0, 25), new ReglaFalsa(0, 10)));

        ResultadoEvaluacion r = motor.evaluar(cliente(), prestamo());
        assertEquals(0, r.score());
        assertEquals(MotorScoring.RECHAZADO, r.decision());
    }

    @Test
    @DisplayName("El promedio es PONDERADO, no simple: 100*40 + 0*60 = 40, no 50")
    void promedioEsPonderado() {
        MotorScoring motor = new MotorScoring(List.of(
                new ReglaFalsa(100, 40), new ReglaFalsa(0, 60)));

        ResultadoEvaluacion r = motor.evaluar(cliente(), prestamo());
        assertEquals(40, r.score());
        assertEquals(MotorScoring.RECHAZADO, r.decision());
    }

    @Test
    @DisplayName("Umbral: score exactamente 60 aprueba (>=)")
    void umbralInclusivo() {
        assertEquals(MotorScoring.APROBADO,
                new MotorScoring(List.of(new ReglaFalsa(60, 100))).evaluar(cliente(), prestamo()).decision());
        assertEquals(MotorScoring.RECHAZADO,
                new MotorScoring(List.of(new ReglaFalsa(59, 100))).evaluar(cliente(), prestamo()).decision());
    }

    @Test
    void listaDeReglasVaciaRechazada() {
        assertThrows(IllegalArgumentException.class, () -> new MotorScoring(List.of()));
    }

    @Test
    @DisplayName("Con las 4 reglas reales: cliente solvente e historial BUENO → APROBADO")
    void integracionReglasReales() {
        MotorScoring motor = new MotorScoring(List.of(
                new ReglaCapacidadPago(), new ReglaIngreso(), new ReglaHistorial(), new ReglaEdad()));

        ResultadoEvaluacion r = motor.evaluar(cliente(), prestamo());
        assertEquals(MotorScoring.APROBADO, r.decision());
    }
}
