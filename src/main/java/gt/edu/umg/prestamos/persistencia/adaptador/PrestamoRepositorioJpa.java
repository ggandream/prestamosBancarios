package gt.edu.umg.prestamos.persistencia.adaptador;

import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;
import gt.edu.umg.prestamos.persistencia.entidad.ClienteEntity;
import gt.edu.umg.prestamos.persistencia.entidad.PrestamoEntity;
import gt.edu.umg.prestamos.persistencia.entidad.TipoEstado;
import gt.edu.umg.prestamos.persistencia.mapper.EstadoMapper;
import gt.edu.umg.prestamos.persistencia.mapper.PrestamoMapper;
import gt.edu.umg.prestamos.persistencia.repositorio.ClienteJpaRepository;
import gt.edu.umg.prestamos.persistencia.repositorio.PrestamoJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de persistencia de préstamos: expone operaciones en términos del dominio
 * ({@link Prestamo}). Resuelve la {@link ClienteEntity} gestionada antes de mapear, de
 * modo que la FK {@code cliente_id} apunte a un cliente ya existente (integridad
 * referencial) sin cascadear su persistencia.
 */
@Component
public class PrestamoRepositorioJpa {

    private final PrestamoJpaRepository repositorio;
    private final ClienteJpaRepository clienteRepositorio;
    private final PrestamoMapper mapper;
    private final EstadoMapper estadoMapper;

    public PrestamoRepositorioJpa(PrestamoJpaRepository repositorio,
                                  ClienteJpaRepository clienteRepositorio,
                                  PrestamoMapper mapper,
                                  EstadoMapper estadoMapper) {
        this.repositorio = repositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.mapper = mapper;
        this.estadoMapper = estadoMapper;
    }

    @Transactional
    public Prestamo guardar(Prestamo prestamo) {
        UUID clienteId = prestamo.getCliente().getId();
        ClienteEntity cliente = clienteRepositorio.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se puede guardar el préstamo: el cliente " + clienteId + " no existe"));
        PrestamoEntity guardado = repositorio.save(mapper.aEntidad(prestamo, cliente));
        return mapper.aDominio(guardado);
    }

    @Transactional(readOnly = true)
    public Optional<Prestamo> buscarPorId(UUID id) {
        return repositorio.findById(id).map(mapper::aDominio);
    }

    @Transactional(readOnly = true)
    public List<Prestamo> buscarPorCliente(UUID clienteId) {
        return repositorio.findByClienteId(clienteId).stream().map(mapper::aDominio).toList();
    }

    @Transactional(readOnly = true)
    public List<Prestamo> buscarPorEstado(TipoEstado tipo) {
        return repositorio.findByEstadoTipo(tipo).stream().map(mapper::aDominio).toList();
    }

    @Transactional(readOnly = true)
    public List<Prestamo> buscarPorTipo(Class<? extends PrestamoEntity> tipo) {
        return repositorio.findByTipo(tipo).stream().map(mapper::aDominio).toList();
    }

    @Transactional(readOnly = true)
    public List<Prestamo> buscarTodos() {
        return repositorio.findAll().stream().map(mapper::aDominio).toList();
    }

    /**
     * Actualiza únicamente el estado de un préstamo ya persistido, sin regenerar el
     * plan de cuotas (regenerarlo con {@link #guardar(Prestamo)} chocaría con la
     * restricción única {@code uk_cuota_prestamo_numero}).
     *
     * @throws IllegalArgumentException si el préstamo no existe
     */
    @Transactional
    public Prestamo actualizarEstado(UUID id, EstadoPrestamo estado) {
        PrestamoEntity entity = repositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se puede actualizar el estado: el préstamo " + id + " no existe"));
        entity.setEstado(estadoMapper.aEmbeddable(estado));
        return mapper.aDominio(entity);
    }
}
