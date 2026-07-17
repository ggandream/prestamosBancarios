package gt.edu.umg.prestamos.config;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.SectorIndustria;
import gt.edu.umg.prestamos.dominio.cliente.TipoEmpleo;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;
import gt.edu.umg.prestamos.persistencia.adaptador.PrestamoRepositorioJpa;
import gt.edu.umg.prestamos.servicio.ClienteService;
import gt.edu.umg.prestamos.servicio.SolicitudService;
import gt.edu.umg.prestamos.servicio.comando.ComandoCrearSolicitud;
import gt.edu.umg.prestamos.servicio.comando.ComandoRegistrarCliente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Datos semilla para desarrollo/demostración (solo perfil {@code dev}). Crea clientes
 * y solicitudes representativas usando los servicios de aplicación, de modo que el
 * flujo sembrado es el mismo que el de la API: cada solicitud creada se evalúa en
 * segundo plano vía {@code ListenerEvaluacion} (Fase 4) y el seed espera el resultado
 * antes de desembolsar. Es idempotente: si ya hay clientes en la base no vuelve a
 * sembrar.
 *
 * <p>Deja la cartera con los escenarios del happy path: un préstamo desembolsado, uno
 * rechazado, uno aprobado sin desembolsar y uno en mora (la mora se marca aquí con la
 * transición legal Desembolsado → EnMora, porque la lógica de cuotas vencidas está
 * fuera de alcance; así el reporte de cartera muestra un índice de mora real).
 */
@Component
@Profile("dev")
public class CargadorDatosSemilla implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CargadorDatosSemilla.class);
    private static final long ESPERA_MAXIMA_MS = 15_000;
    private static final long INTERVALO_SONDEO_MS = 200;

    private final ClienteService clientes;
    private final SolicitudService solicitudes;
    private final PrestamoRepositorioJpa prestamos;

    public CargadorDatosSemilla(ClienteService clientes, SolicitudService solicitudes,
                                PrestamoRepositorioJpa prestamos) {
        this.clientes = clientes;
        this.solicitudes = solicitudes;
        this.prestamos = prestamos;
    }

    @Override
    public void run(String... args) throws InterruptedException {
        if (!clientes.listar().isEmpty()) {
            log.info("seed_omitido: la base ya contiene clientes");
            return;
        }

        // Perfil sólido: su solicitud se aprueba en background y luego se desembolsa.
        Cliente maria = clientes.registrar(new ComandoRegistrarCliente.Individual(
                "María López", "2547896320101", "maria.lopez@example.com",
                HistorialCrediticio.BUENO,
                new BigDecimal("8000.00"), TipoEmpleo.FORMAL, 6));
        Prestamo deMaria = solicitudes.crear(new ComandoCrearSolicitud.Personal(
                maria.getId(), new BigDecimal("50000.00"), 60, new BigDecimal("0.12")));
        esperarEvaluacion(deMaria.getId());
        solicitudes.desembolsar(deMaria.getId());

        // Perfil débil: su solicitud queda RECHAZADA por el listener.
        Cliente carlos = clientes.registrar(new ComandoRegistrarCliente.Individual(
                "Carlos Pérez", "1893456780101", "carlos.perez@example.com",
                HistorialCrediticio.MALO,
                new BigDecimal("1200.00"), TipoEmpleo.INFORMAL, 0));
        Prestamo deCarlos = solicitudes.crear(new ComandoCrearSolicitud.Personal(
                carlos.getId(), new BigDecimal("30000.00"), 24, new BigDecimal("0.15")));
        esperarEvaluacion(deCarlos.getId());

        // Cliente empresarial: su solicitud automotriz queda APROBADA sin desembolsar.
        Cliente ferreteria = clientes.registrar(new ComandoRegistrarCliente.Empresarial(
                "Ferretería El Tornillo S.A.", "9876543210505", "contacto@eltornillo.com.gt",
                HistorialCrediticio.BUENO,
                new BigDecimal("600000.00"), "5489632-1", SectorIndustria.COMERCIO, 8));
        Prestamo deFerreteria = solicitudes.crear(new ComandoCrearSolicitud.Automotriz(
                ferreteria.getId(), new BigDecimal("80000.00"), 48, new BigDecimal("0.10"),
                "Camión Hino Serie 300, modelo 2024", new BigDecimal("0.15")));
        esperarEvaluacion(deFerreteria.getId());

        // Cliente que cae en mora tras el desembolso (para el índice de mora del reporte).
        Cliente diego = clientes.registrar(new ComandoRegistrarCliente.Individual(
                "Diego Martínez", "3320145670101", "diego.martinez@example.com",
                HistorialCrediticio.REGULAR,
                new BigDecimal("6000.00"), TipoEmpleo.FORMAL, 3));
        Prestamo deDiego = solicitudes.crear(new ComandoCrearSolicitud.Personal(
                diego.getId(), new BigDecimal("20000.00"), 36, new BigDecimal("0.14")));
        esperarEvaluacion(deDiego.getId());
        Prestamo desembolsado = solicitudes.desembolsar(deDiego.getId());
        desembolsado.cambiarEstado(new EstadoPrestamo.EnMora(45, new BigDecimal("2050.80")));
        prestamos.actualizarEstado(desembolsado.getId(), desembolsado.getEstado());

        log.info("seed_completado: {} clientes, {} préstamos",
                clientes.listar().size(), solicitudes.listar().size());
    }

    /** Sondea hasta que la evaluación asíncrona deje la solicitud en su estado final. */
    private void esperarEvaluacion(UUID prestamoId) throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MAXIMA_MS;
        while (System.currentTimeMillis() < limite) {
            EstadoPrestamo estado = solicitudes.buscarPorId(prestamoId).getEstado();
            if (estado instanceof EstadoPrestamo.Aprobado
                    || estado instanceof EstadoPrestamo.Rechazado) {
                return;
            }
            Thread.sleep(INTERVALO_SONDEO_MS);
        }
        throw new IllegalStateException(
                "La evaluación asíncrona del préstamo " + prestamoId + " no terminó a tiempo");
    }
}
