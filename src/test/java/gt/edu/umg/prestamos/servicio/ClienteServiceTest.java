package gt.edu.umg.prestamos.servicio;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.cliente.ClienteEmpresarial;
import gt.edu.umg.prestamos.dominio.cliente.ClienteIndividual;
import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.SectorIndustria;
import gt.edu.umg.prestamos.dominio.cliente.TipoEmpleo;
import gt.edu.umg.prestamos.persistencia.adaptador.ClienteRepositorioJpa;
import gt.edu.umg.prestamos.servicio.comando.ComandoRegistrarCliente;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests unitarios de {@link ClienteService} con el repositorio simulado. */
@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepositorioJpa repositorio;

    @InjectMocks
    private ClienteService servicio;

    private static ComandoRegistrarCliente.Individual comandoIndividual() {
        return new ComandoRegistrarCliente.Individual(
                "María López", "2547896320101", "maria@example.com",
                HistorialCrediticio.BUENO, new BigDecimal("8000.00"), TipoEmpleo.FORMAL, 6);
    }

    @Test
    void registraClienteIndividualConSusDatos() {
        when(repositorio.existePorDocumento("2547896320101")).thenReturn(false);
        when(repositorio.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Cliente cliente = servicio.registrar(comandoIndividual());

        assertThat(cliente).isInstanceOf(ClienteIndividual.class);
        assertThat(cliente.getNombre()).isEqualTo("María López");
        assertThat(((ClienteIndividual) cliente).getTipoEmpleo()).isEqualTo(TipoEmpleo.FORMAL);
        verify(repositorio).guardar(any(ClienteIndividual.class));
    }

    @Test
    void registraClienteEmpresarialConSusDatos() {
        when(repositorio.existePorDocumento("9876543210505")).thenReturn(false);
        when(repositorio.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Cliente cliente = servicio.registrar(new ComandoRegistrarCliente.Empresarial(
                "Ferretería El Tornillo S.A.", "9876543210505", "contacto@tornillo.com",
                HistorialCrediticio.BUENO, new BigDecimal("600000.00"), "5489632-1",
                SectorIndustria.COMERCIO, 8));

        assertThat(cliente).isInstanceOf(ClienteEmpresarial.class);
        assertThat(((ClienteEmpresarial) cliente).getNit()).isEqualTo("5489632-1");
    }

    @Test
    void rechazaDocumentoDuplicado() {
        when(repositorio.existePorDocumento("2547896320101")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(comandoIndividual()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("documento");
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void buscarPorIdInexistenteLanzaNoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repositorio.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.buscarPorId(id))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
