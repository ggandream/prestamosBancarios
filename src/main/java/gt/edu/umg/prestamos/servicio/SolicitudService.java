package gt.edu.umg.prestamos.servicio;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoAutomotriz;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoHipotecario;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoPersonal;
import gt.edu.umg.prestamos.persistencia.adaptador.ClienteRepositorioJpa;
import gt.edu.umg.prestamos.persistencia.adaptador.PrestamoRepositorioJpa;
import gt.edu.umg.prestamos.servicio.comando.ComandoCrearSolicitud;
import gt.edu.umg.prestamos.servicio.evento.EventoPrestamoDesembolsado;
import gt.edu.umg.prestamos.servicio.evento.EventoSolicitudCreada;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de aplicación para solicitudes de préstamo. Crea el préstamo en estado
 * {@code Borrador} asociado a un cliente ya registrado y publica
 * {@link EventoSolicitudCreada}, que dispara la evaluación asíncrona (Fase 4, patrón
 * Observer). También gestiona el desembolso de préstamos aprobados.
 */
@Service
public class SolicitudService {

    private final ClienteRepositorioJpa clientes;
    private final PrestamoRepositorioJpa prestamos;
    private final ApplicationEventPublisher publicador;

    public SolicitudService(ClienteRepositorioJpa clientes, PrestamoRepositorioJpa prestamos,
                            ApplicationEventPublisher publicador) {
        this.clientes = clientes;
        this.prestamos = prestamos;
        this.publicador = publicador;
    }

    /**
     * Crea una solicitud de préstamo en estado Borrador y publica el evento que
     * inicia su evaluación en segundo plano.
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
        Prestamo guardado = prestamos.guardar(prestamo);
        publicador.publishEvent(new EventoSolicitudCreada(guardado.getId()));
        return guardado;
    }

    /**
     * Desembolsa un préstamo aprobado (Aprobado → Desembolsado) y publica
     * {@link EventoPrestamoDesembolsado}.
     *
     * @throws RecursoNoEncontradoException si el préstamo no existe
     * @throws gt.edu.umg.prestamos.dominio.estado.TransicionInvalidaException si el
     *         préstamo no está aprobado (la API la traduce a HTTP 409)
     */
    public Prestamo desembolsar(UUID id) {
        Prestamo prestamo = buscarPorId(id);
        EstadoPrestamo.Desembolsado desembolso =
                new EstadoPrestamo.Desembolsado(LocalDateTime.now(), prestamo.getMonto());
        prestamo.cambiarEstado(desembolso);
        Prestamo actualizado = prestamos.actualizarEstado(id, desembolso);
        publicador.publishEvent(new EventoPrestamoDesembolsado(id, prestamo.getMonto()));
        return actualizado;
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
