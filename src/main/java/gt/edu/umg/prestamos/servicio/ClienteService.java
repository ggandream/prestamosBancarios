package gt.edu.umg.prestamos.servicio;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.cliente.ClienteEmpresarial;
import gt.edu.umg.prestamos.dominio.cliente.ClienteIndividual;
import gt.edu.umg.prestamos.persistencia.adaptador.ClienteRepositorioJpa;
import gt.edu.umg.prestamos.servicio.comando.ComandoRegistrarCliente;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de aplicación para el registro y consulta de clientes. Construye el
 * subtipo de dominio a partir del comando (las validaciones de negocio viven en los
 * constructores del dominio) y delega la persistencia al adaptador de la Fase 2.
 */
@Service
public class ClienteService {

    private final ClienteRepositorioJpa clientes;

    public ClienteService(ClienteRepositorioJpa clientes) {
        this.clientes = clientes;
    }

    /**
     * Registra un cliente nuevo.
     *
     * @throws IllegalArgumentException si ya existe un cliente con el mismo documento
     *         o si los datos violan las validaciones del dominio
     */
    public Cliente registrar(ComandoRegistrarCliente comando) {
        if (clientes.existePorDocumento(comando.documento())) {
            throw new IllegalArgumentException(
                    "Ya existe un cliente con el documento " + comando.documento());
        }
        Cliente cliente = switch (comando) {
            case ComandoRegistrarCliente.Individual i -> new ClienteIndividual(
                    UUID.randomUUID(), i.nombre(), i.documento(), i.email(),
                    LocalDate.now(), i.historial(),
                    i.salarioMensual(), i.tipoEmpleo(), i.antiguedadLaboral());
            case ComandoRegistrarCliente.Empresarial e -> new ClienteEmpresarial(
                    UUID.randomUUID(), e.nombre(), e.documento(), e.email(),
                    LocalDate.now(), e.historial(),
                    e.facturacionAnual(), e.nit(), e.sector(), e.antiguedadNit());
        };
        return clientes.guardar(cliente);
    }

    /**
     * Busca un cliente por su identificador.
     *
     * @throws RecursoNoEncontradoException si no existe
     */
    public Cliente buscarPorId(UUID id) {
        return clientes.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + id));
    }

    /** Lista todos los clientes registrados. */
    public List<Cliente> listar() {
        return clientes.buscarTodos();
    }
}
