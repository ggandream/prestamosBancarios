package gt.edu.umg.prestamos.servicio;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.cliente.ClienteIndividual;
import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.TipoEmpleo;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoHipotecario;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoPersonal;
import gt.edu.umg.prestamos.persistencia.adaptador.ClienteRepositorioJpa;
import gt.edu.umg.prestamos.persistencia.adaptador.PrestamoRepositorioJpa;
import gt.edu.umg.prestamos.servicio.comando.ComandoCrearSolicitud;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests unitarios de {@link SolicitudService} con los repositorios simulados. */
@ExtendWith(MockitoExtension.class)
class SolicitudServiceTest {

    @Mock
    private ClienteRepositorioJpa clientes;

    @Mock
    private PrestamoRepositorioJpa prestamos;

    @InjectMocks
    private SolicitudService servicio;

    private static Cliente cliente() {
        return new ClienteIndividual(UUID.randomUUID(), "María López", "254789",
                "maria@example.com", LocalDate.of(2020, 1, 15), HistorialCrediticio.BUENO,
                new BigDecimal("8000.00"), TipoEmpleo.FORMAL, 6);
    }

    @Test
    void creaSolicitudPersonalEnEstadoBorrador() {
        Cliente cliente = cliente();
        when(clientes.buscarPorId(cliente.getId())).thenReturn(Optional.of(cliente));
        when(prestamos.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Prestamo prestamo = servicio.crear(new ComandoCrearSolicitud.Personal(
                cliente.getId(), new BigDecimal("50000.00"), 60, new BigDecimal("0.12")));

        assertThat(prestamo).isInstanceOf(PrestamoPersonal.class);
        assertThat(prestamo.getEstado()).isInstanceOf(EstadoPrestamo.Borrador.class);
        assertThat(prestamo.getCliente()).isSameAs(cliente);
    }

    @Test
    void creaSolicitudHipotecariaConGarantia() {
        Cliente cliente = cliente();
        when(clientes.buscarPorId(cliente.getId())).thenReturn(Optional.of(cliente));
        when(prestamos.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Prestamo prestamo = servicio.crear(new ComandoCrearSolicitud.Hipotecaria(
                cliente.getId(), new BigDecimal("200000.00"), 240, new BigDecimal("0.09"),
                "Casa en zona 10", new BigDecimal("300000.00")));

        assertThat(prestamo).isInstanceOf(PrestamoHipotecario.class);
        assertThat(((PrestamoHipotecario) prestamo).getAvaluo())
                .isEqualByComparingTo("300000.00");
    }

    @Test
    void clienteInexistenteLanzaNoEncontrado() {
        UUID clienteId = UUID.randomUUID();
        when(clientes.buscarPorId(clienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(new ComandoCrearSolicitud.Personal(
                clienteId, new BigDecimal("50000.00"), 60, new BigDecimal("0.12"))))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(prestamos, never()).guardar(any());
    }
}
