package gt.edu.umg.prestamos.servicio;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.cliente.ClienteIndividual;
import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.TipoEmpleo;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo;
import gt.edu.umg.prestamos.dominio.estado.TransicionInvalidaException;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoPersonal;
import gt.edu.umg.prestamos.dominio.scoring.MotorScoring;
import gt.edu.umg.prestamos.dominio.scoring.ReglaCapacidadPago;
import gt.edu.umg.prestamos.dominio.scoring.ReglaEdad;
import gt.edu.umg.prestamos.dominio.scoring.ReglaHistorial;
import gt.edu.umg.prestamos.dominio.scoring.ReglaIngreso;
import gt.edu.umg.prestamos.persistencia.adaptador.PrestamoRepositorioJpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link EvaluacionService} usando el {@link MotorScoring} real con
 * las 4 reglas (el scoring es determinista: mismo input, mismo output) y el
 * repositorio simulado.
 */
@ExtendWith(MockitoExtension.class)
class EvaluacionServiceTest {

    @Mock
    private PrestamoRepositorioJpa prestamos;

    private EvaluacionService servicio;

    @BeforeEach
    void configurar() {
        MotorScoring motor = new MotorScoring(List.of(
                new ReglaCapacidadPago(), new ReglaIngreso(),
                new ReglaHistorial(), new ReglaEdad()));
        servicio = new EvaluacionService(prestamos, motor);
    }

    private static Cliente clienteSolido() {
        return new ClienteIndividual(UUID.randomUUID(), "María López", "254789",
                "maria@example.com", LocalDate.of(2019, 3, 1), HistorialCrediticio.BUENO,
                new BigDecimal("8000.00"), TipoEmpleo.FORMAL, 6);
    }

    private static Cliente clienteDebil() {
        return new ClienteIndividual(UUID.randomUUID(), "Carlos Pérez", "189345",
                "carlos@example.com", LocalDate.of(2025, 1, 1), HistorialCrediticio.MALO,
                new BigDecimal("1200.00"), TipoEmpleo.INFORMAL, 0);
    }

    private static Prestamo prestamoDe(Cliente cliente) {
        return new PrestamoPersonal(UUID.randomUUID(), cliente,
                new BigDecimal("50000.00"), 60, new BigDecimal("0.12"),
                LocalDate.of(2026, 7, 1));
    }

    @Test
    void apruebaSolicitudDeClienteSolido() {
        Prestamo prestamo = prestamoDe(clienteSolido());
        when(prestamos.buscarPorId(prestamo.getId())).thenReturn(Optional.of(prestamo));
        when(prestamos.actualizarEstado(eq(prestamo.getId()), any())).thenReturn(prestamo);

        var evaluacion = servicio.evaluar(prestamo.getId());

        assertThat(evaluacion.resultado().decision()).isEqualTo(MotorScoring.APROBADO);
        assertThat(evaluacion.resultado().score())
                .isGreaterThanOrEqualTo(MotorScoring.UMBRAL_APROBACION);
        verify(prestamos).actualizarEstado(eq(prestamo.getId()),
                any(EstadoPrestamo.Aprobado.class));
    }

    @Test
    void rechazaSolicitudDeClienteDebil() {
        Prestamo prestamo = prestamoDe(clienteDebil());
        when(prestamos.buscarPorId(prestamo.getId())).thenReturn(Optional.of(prestamo));
        when(prestamos.actualizarEstado(eq(prestamo.getId()), any())).thenReturn(prestamo);

        var evaluacion = servicio.evaluar(prestamo.getId());

        assertThat(evaluacion.resultado().decision()).isEqualTo(MotorScoring.RECHAZADO);
        verify(prestamos).actualizarEstado(eq(prestamo.getId()),
                any(EstadoPrestamo.Rechazado.class));
    }

    @Test
    void solicitudYaEvaluadaLanzaTransicionInvalida() {
        Prestamo prestamo = prestamoDe(clienteSolido());
        prestamo.cambiarEstado(new EstadoPrestamo.EnEvaluacion(LocalDateTime.now(), "otro"));
        when(prestamos.buscarPorId(prestamo.getId())).thenReturn(Optional.of(prestamo));

        assertThatThrownBy(() -> servicio.evaluar(prestamo.getId()))
                .isInstanceOf(TransicionInvalidaException.class);
        verify(prestamos, never()).actualizarEstado(any(), any());
    }

    @Test
    void prestamoInexistenteLanzaNoEncontrado() {
        UUID id = UUID.randomUUID();
        when(prestamos.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.evaluar(id))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
