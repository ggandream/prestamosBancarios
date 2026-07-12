package gt.edu.umg.prestamos.persistencia;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.cliente.ClienteEmpresarial;
import gt.edu.umg.prestamos.dominio.cliente.ClienteIndividual;
import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.SectorIndustria;
import gt.edu.umg.prestamos.dominio.cliente.TipoEmpleo;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Aprobado;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Borrador;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Desembolsado;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.EnEvaluacion;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.EnMora;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Pagado;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Rechazado;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoAutomotriz;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoHipotecario;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoPersonal;
import gt.edu.umg.prestamos.persistencia.adaptador.ClienteRepositorioJpa;
import gt.edu.umg.prestamos.persistencia.adaptador.PrestamoRepositorioJpa;
import gt.edu.umg.prestamos.persistencia.entidad.PrestamoAutomotrizEntity;
import gt.edu.umg.prestamos.persistencia.entidad.PrestamoEntity;
import gt.edu.umg.prestamos.persistencia.entidad.TipoEstado;
import gt.edu.umg.prestamos.persistencia.mapper.ClienteMapper;
import gt.edu.umg.prestamos.persistencia.mapper.CuotaMapper;
import gt.edu.umg.prestamos.persistencia.mapper.EstadoMapper;
import gt.edu.umg.prestamos.persistencia.mapper.PrestamoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración de la capa de persistencia (Fase 2). Verifican el ciclo completo
 * guardar → leer → verificar contra H2, a través de los adaptadores de dominio (los
 * mismos que consumirá la capa de servicio), cubriendo el criterio de aceptación:
 * los 2 tipos de cliente, los 3 tipos de préstamo y los 7 estados.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ClienteMapper.class, EstadoMapper.class, CuotaMapper.class, PrestamoMapper.class,
        ClienteRepositorioJpa.class, PrestamoRepositorioJpa.class})
class PersistenciaIntegracionTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ClienteRepositorioJpa clienteRepo;

    @Autowired
    private PrestamoRepositorioJpa prestamoRepo;

    // --- clientes -------------------------------------------------------------------

    @Test
    void guardaYRecuperaClienteIndividual() {
        var original = clienteIndividual("CUI-001", "IND-001");

        clienteRepo.guardar(original);
        recargarContexto();
        Cliente recuperado = clienteRepo.buscarPorId(original.getId()).orElseThrow();

        assertThat(recuperado).isInstanceOf(ClienteIndividual.class);
        var ci = (ClienteIndividual) recuperado;
        assertThat(ci.getNombre()).isEqualTo("Ana Pérez");
        assertThat(ci.getDocumento()).isEqualTo("CUI-001");
        assertThat(ci.getSalarioMensual()).isEqualByComparingTo("8500.00");
        assertThat(ci.getTipoEmpleo()).isEqualTo(TipoEmpleo.FORMAL);
        assertThat(ci.getAntiguedadLaboral()).isEqualTo(4);
        assertThat(ci.getHistorial()).isEqualTo(HistorialCrediticio.BUENO);
    }

    @Test
    void guardaYRecuperaClienteEmpresarial() {
        var original = clienteEmpresarial("NIT-100", "DOC-100");

        clienteRepo.guardar(original);
        recargarContexto();
        Cliente recuperado = clienteRepo.buscarPorId(original.getId()).orElseThrow();

        assertThat(recuperado).isInstanceOf(ClienteEmpresarial.class);
        var ce = (ClienteEmpresarial) recuperado;
        assertThat(ce.getNit()).isEqualTo("NIT-100");
        assertThat(ce.getFacturacionAnual()).isEqualByComparingTo("1200000.00");
        assertThat(ce.getSector()).isEqualTo(SectorIndustria.SERVICIOS);
        assertThat(ce.getAntiguedadNit()).isEqualTo(7);
    }

    @Test
    void reconstruyeElSubtipoConsultandoPorDocumento() {
        clienteRepo.guardar(clienteEmpresarial("NIT-777", "DOC-777"));
        recargarContexto();

        Cliente recuperado = clienteRepo.buscarPorDocumento("DOC-777").orElseThrow();
        assertThat(recuperado).isInstanceOf(ClienteEmpresarial.class);
    }

    // --- préstamos ------------------------------------------------------------------

    @Test
    void guardaYRecuperaPrestamoPersonalConSuPlanDeCuotas() {
        var cliente = clienteRepo.guardar(clienteIndividual("CUI-P", "IND-P"));
        var prestamo = new PrestamoPersonal(UUID.randomUUID(), cliente,
                new BigDecimal("50000.00"), 12, new BigDecimal("0.12"), LocalDate.of(2026, 1, 15));

        prestamoRepo.guardar(prestamo);
        recargarContexto();

        Prestamo recuperado = prestamoRepo.buscarPorId(prestamo.getId()).orElseThrow();
        assertThat(recuperado).isInstanceOf(PrestamoPersonal.class);
        assertThat(recuperado.getMonto()).isEqualByComparingTo("50000.00");
        assertThat(recuperado.getPlazoMeses()).isEqualTo(12);
        assertThat(recuperado.getCliente().getId()).isEqualTo(cliente.getId());

        // el plan de cuotas se persiste como snapshot (12 cuotas)
        PrestamoEntity entity = em.getEntityManager().find(PrestamoEntity.class, prestamo.getId());
        assertThat(entity.getCuotas()).hasSize(12);
    }

    @Test
    void guardaYRecuperaPrestamoHipotecario() {
        var cliente = clienteRepo.guardar(clienteEmpresarial("NIT-H", "DOC-H"));
        var prestamo = new PrestamoHipotecario(UUID.randomUUID(), cliente,
                new BigDecimal("400000.00"), 240, new BigDecimal("0.09"), LocalDate.of(2026, 1, 15),
                "Casa zona 15", new BigDecimal("600000.00"));

        prestamoRepo.guardar(prestamo);
        recargarContexto();

        Prestamo recuperado = prestamoRepo.buscarPorId(prestamo.getId()).orElseThrow();
        assertThat(recuperado).isInstanceOf(PrestamoHipotecario.class);
        assertThat(((PrestamoHipotecario) recuperado).getAvaluo()).isEqualByComparingTo("600000.00");
        assertThat(((PrestamoHipotecario) recuperado).getDescripcionGarantia()).isEqualTo("Casa zona 15");
    }

    @Test
    void guardaYRecuperaPrestamoAutomotriz() {
        var cliente = clienteRepo.guardar(clienteIndividual("CUI-A", "IND-A"));
        var prestamo = new PrestamoAutomotriz(UUID.randomUUID(), cliente,
                new BigDecimal("120000.00"), 48, new BigDecimal("0.15"), LocalDate.of(2026, 1, 15),
                "Toyota Hilux 2026", new BigDecimal("0.20"));

        prestamoRepo.guardar(prestamo);
        recargarContexto();

        Prestamo recuperado = prestamoRepo.buscarPorId(prestamo.getId()).orElseThrow();
        assertThat(recuperado).isInstanceOf(PrestamoAutomotriz.class);
        assertThat(((PrestamoAutomotriz) recuperado).getVehiculo()).isEqualTo("Toyota Hilux 2026");

        // consulta por tipo (discriminador de la jerarquía JOINED)
        var automotrices = prestamoRepo.buscarPorTipo(PrestamoAutomotrizEntity.class);
        assertThat(automotrices).extracting(Prestamo::getId).contains(prestamo.getId());
    }

    // --- los 7 estados --------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("losSieteEstados")
    void persisteYReconstruyeCadaEstado(EstadoPrestamo estado) {
        var cliente = clienteRepo.guardar(clienteIndividual(
                "CUI-" + UUID.randomUUID(), "IND-" + UUID.randomUUID()));
        var prestamo = new PrestamoPersonal(UUID.randomUUID(), cliente,
                new BigDecimal("30000.00"), 12, new BigDecimal("0.12"), LocalDate.of(2026, 1, 15));
        prestamo.restaurarEstado(estado);

        prestamoRepo.guardar(prestamo);
        recargarContexto();

        Prestamo recuperado = prestamoRepo.buscarPorId(prestamo.getId()).orElseThrow();
        // los records de EstadoPrestamo tienen igualdad por valor: comparación directa
        assertThat(recuperado.getEstado()).isEqualTo(estado);
    }

    @Test
    void consultaPrestamosPorEstado() {
        var cliente = clienteRepo.guardar(clienteIndividual("CUI-E", "IND-E"));
        var prestamo = new PrestamoPersonal(UUID.randomUUID(), cliente,
                new BigDecimal("30000.00"), 12, new BigDecimal("0.12"), LocalDate.of(2026, 1, 15));
        prestamo.restaurarEstado(new Aprobado(LocalDateTime.of(2026, 1, 12, 9, 0), 75));
        prestamoRepo.guardar(prestamo);
        recargarContexto();

        var aprobados = prestamoRepo.buscarPorEstado(TipoEstado.APROBADO);
        assertThat(aprobados).extracting(Prestamo::getId).contains(prestamo.getId());
    }

    static Stream<EstadoPrestamo> losSieteEstados() {
        return Stream.of(
                new Borrador(LocalDateTime.of(2026, 1, 10, 9, 0)),
                new EnEvaluacion(LocalDateTime.of(2026, 1, 11, 9, 0), "sistema"),
                new Aprobado(LocalDateTime.of(2026, 1, 12, 9, 0), 75),
                new Rechazado(LocalDateTime.of(2026, 1, 12, 9, 0), "score insuficiente"),
                new Desembolsado(LocalDateTime.of(2026, 1, 13, 9, 0), new BigDecimal("30000.00")),
                new Pagado(LocalDateTime.of(2026, 6, 13, 9, 0)),
                new EnMora(45, new BigDecimal("1200.00")));
    }

    // --- helpers --------------------------------------------------------------------

    private void recargarContexto() {
        em.flush();
        em.clear();
    }

    private static ClienteIndividual clienteIndividual(String documento, String sufijo) {
        return new ClienteIndividual(UUID.randomUUID(), "Ana Pérez", documento,
                "ana." + sufijo.toLowerCase() + "@example.com", LocalDate.of(2022, 1, 1),
                HistorialCrediticio.BUENO, new BigDecimal("8500.00"), TipoEmpleo.FORMAL, 4);
    }

    private static ClienteEmpresarial clienteEmpresarial(String nit, String documento) {
        return new ClienteEmpresarial(UUID.randomUUID(), "Comercial XYZ", documento,
                "contacto." + documento.toLowerCase() + "@example.com", LocalDate.of(2019, 6, 1),
                HistorialCrediticio.REGULAR, new BigDecimal("1200000.00"), nit,
                SectorIndustria.SERVICIOS, 7);
    }
}
