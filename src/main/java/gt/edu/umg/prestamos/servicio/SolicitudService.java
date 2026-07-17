package gt.edu.umg.prestamos.servicio;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoAutomotriz;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoHipotecario;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoPersonal;
import gt.edu.umg.prestamos.persistencia.adaptador.ClienteRepositorioJpa;
import gt.edu.umg.prestamos.persistencia.adaptador.PrestamoRepositorioJpa;
import gt.edu.umg.prestamos.servicio.comando.ComandoCrearSolicitud;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de aplicación para solicitudes de préstamo. Crea el préstamo en estado
 * {@code Borrador} (estado inicial que fija el constructor del dominio) asociado a un
 * cliente ya registrado, y expone las consultas de préstamos.
 */
@Service
public class SolicitudService {

    private final ClienteRepositorioJpa clientes;
    private final PrestamoRepositorioJpa prestamos;

    public SolicitudService(ClienteRepositorioJpa clientes, PrestamoRepositorioJpa prestamos) {
        this.clientes = clientes;
        this.prestamos = prestamos;
    }

    /**
     * Crea una solicitud de préstamo en estado Borrador.
     *
     * @throws RecursoNoEncontradoException si el cliente no existe
     * @throws IllegalArgumentException si los datos violan las validaciones del dominio
     */
    public Prestamo crear(ComandoCrearSolicitud comando) {
        Cliente cliente = clientes.buscarPorId(comando.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cliente no encontrado: " + comando.clienteId()));
        Prestamo prestamo = switch (comando) {
            case ComandoCrearSolicitud.Personal p -> new PrestamoPersonal(
                    UUID.randomUUID(), cliente, p.monto(), p.plazoMeses(),
                    p.tasaAnual(), LocalDate.now());
            case ComandoCrearSolicitud.Hipotecaria h -> new PrestamoHipotecario(
                    UUID.randomUUID(), cliente, h.monto(), h.plazoMeses(),
                    h.tasaAnual(), LocalDate.now(), h.descripcionGarantia(), h.avaluo());
            case ComandoCrearSolicitud.Automotriz a -> new PrestamoAutomotriz(
                    UUID.randomUUID(), cliente, a.monto(), a.plazoMeses(),
                    a.tasaAnual(), LocalDate.now(), a.vehiculo(), a.depreciacionAnual());
        };
        return prestamos.guardar(prestamo);
    }

    /**
     * Busca un préstamo por su identificador.
     *
     * @throws RecursoNoEncontradoException si no existe
     */
    public Prestamo buscarPorId(UUID id) {
        return prestamos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Préstamo no encontrado: " + id));
    }

    /** Lista todos los préstamos registrados. */
    public List<Prestamo> listar() {
        return prestamos.buscarTodos();
    }
}
