package gt.edu.umg.prestamos.config;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.SectorIndustria;
import gt.edu.umg.prestamos.dominio.cliente.TipoEmpleo;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;
import gt.edu.umg.prestamos.servicio.ClienteService;
import gt.edu.umg.prestamos.servicio.EvaluacionService;
import gt.edu.umg.prestamos.servicio.SolicitudService;
import gt.edu.umg.prestamos.servicio.comando.ComandoCrearSolicitud;
import gt.edu.umg.prestamos.servicio.comando.ComandoRegistrarCliente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Datos semilla para desarrollo/demostración (solo perfil {@code dev}). Crea clientes
 * y solicitudes representativas usando los servicios de aplicación, de modo que el
 * flujo sembrado es el mismo que el de la API. Es idempotente: si ya hay clientes en
 * la base no vuelve a sembrar.
 *
 * <p>Deja la base con los tres escenarios del happy path: una solicitud aprobada,
 * una rechazada y una pendiente en Borrador.
 */
@Component
@Profile("dev")
public class CargadorDatosSemilla implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CargadorDatosSemilla.class);

    private final ClienteService clientes;
    private final SolicitudService solicitudes;
    private final EvaluacionService evaluacion;

    public CargadorDatosSemilla(ClienteService clientes, SolicitudService solicitudes,
                                EvaluacionService evaluacion) {
        this.clientes = clientes;
        this.solicitudes = solicitudes;
        this.evaluacion = evaluacion;
    }

    @Override
    public void run(String... args) {
        if (!clientes.listar().isEmpty()) {
            log.info("seed_omitido: la base ya contiene clientes");
            return;
        }

        // Perfil sólido: empleo formal, buen historial → su solicitud queda APROBADA.
        Cliente maria = clientes.registrar(new ComandoRegistrarCliente.Individual(
                "María López", "2547896320101", "maria.lopez@example.com",
                HistorialCrediticio.BUENO,
                new BigDecimal("8000.00"), TipoEmpleo.FORMAL, 6));
        Prestamo personal = solicitudes.crear(new ComandoCrearSolicitud.Personal(
                maria.getId(), new BigDecimal("50000.00"), 60, new BigDecimal("0.12")));
        evaluacion.evaluar(personal.getId());

        // Perfil débil: empleo informal, mal historial → su solicitud queda RECHAZADA.
        Cliente carlos = clientes.registrar(new ComandoRegistrarCliente.Individual(
                "Carlos Pérez", "1893456780101", "carlos.perez@example.com",
                HistorialCrediticio.MALO,
                new BigDecimal("1200.00"), TipoEmpleo.INFORMAL, 0));
        Prestamo rechazado = solicitudes.crear(new ComandoCrearSolicitud.Personal(
                carlos.getId(), new BigDecimal("30000.00"), 24, new BigDecimal("0.15")));
        evaluacion.evaluar(rechazado.getId());

        // Cliente empresarial con solicitud automotriz pendiente (queda en Borrador).
        Cliente ferreteria = clientes.registrar(new ComandoRegistrarCliente.Empresarial(
                "Ferretería El Tornillo S.A.", "9876543210505", "contacto@eltornillo.com.gt",
                HistorialCrediticio.BUENO,
                new BigDecimal("600000.00"), "5489632-1", SectorIndustria.COMERCIO, 8));
        solicitudes.crear(new ComandoCrearSolicitud.Automotriz(
                ferreteria.getId(), new BigDecimal("80000.00"), 48, new BigDecimal("0.10"),
                "Camión Hino Serie 300, modelo 2024", new BigDecimal("0.15")));

        log.info("seed_completado: {} clientes, {} préstamos",
                clientes.listar().size(), solicitudes.listar().size());
    }
}
