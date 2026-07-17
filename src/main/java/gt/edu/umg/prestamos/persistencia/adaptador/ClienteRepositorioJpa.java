package gt.edu.umg.prestamos.persistencia.adaptador;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.persistencia.entidad.ClienteEntity;
import gt.edu.umg.prestamos.persistencia.mapper.ClienteMapper;
import gt.edu.umg.prestamos.persistencia.repositorio.ClienteJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de persistencia de clientes: expone operaciones en términos del dominio
 * ({@link Cliente}) ocultando el {@link ClienteJpaRepository} y las entidades JPA.
 *
 * <p>Es la frontera que la capa de servicio (Fase 3) consumirá; garantiza que las
 * entidades JPA nunca escapen del paquete de persistencia.
 */
@Component
public class ClienteRepositorioJpa {

    private final ClienteJpaRepository repositorio;
    private final ClienteMapper mapper;

    public ClienteRepositorioJpa(ClienteJpaRepository repositorio, ClienteMapper mapper) {
        this.repositorio = repositorio;
        this.mapper = mapper;
    }

    @Transactional
    public Cliente guardar(Cliente cliente) {
        ClienteEntity guardado = repositorio.save(mapper.aEntidad(cliente));
        return mapper.aDominio(guardado);
    }

    @Transactional(readOnly = true)
    public Optional<Cliente> buscarPorId(UUID id) {
        return repositorio.findById(id).map(mapper::aDominio);
    }

    @Transactional(readOnly = true)
    public Optional<Cliente> buscarPorDocumento(String documento) {
        return repositorio.findByDocumento(documento).map(mapper::aDominio);
    }

    @Transactional(readOnly = true)
    public boolean existePorDocumento(String documento) {
        return repositorio.existsByDocumento(documento);
    }

    @Transactional(readOnly = true)
    public List<Cliente> buscarTodos() {
        return repositorio.findAll().stream().map(mapper::aDominio).toList();
    }
}
