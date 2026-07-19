package gt.edu.umg.prestamos;

import com.fasterxml.jackson.databind.ObjectMapper;
import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.TipoEmpleo;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo;
import gt.edu.umg.prestamos.servicio.ClienteService;
import gt.edu.umg.prestamos.servicio.SolicitudService;
import gt.edu.umg.prestamos.servicio.comando.ComandoRegistrarCliente;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de integración de la Fase 4 (H2 en memoria): una solicitud creada por HTTP
 * responde 202 y queda evaluada automáticamente por {@code ListenerEvaluacion} sin
 * bloquear la respuesta; luego se desembolsa y el reporte de cartera refleja los
 * datos reales de la base. La espera del resultado asíncrono se hace con sondeo
 * acotado (sin dependencias adicionales).
 */
@SpringBootTest
@AutoConfigureMockMvc
class FlujoAsincronoIntegracionTest {

    private static final long ESPERA_MAXIMA_MS = 15_000;
    private static final long INTERVALO_SONDEO_MS = 100;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private ClienteService clientes;

    @Autowired
    private SolicitudService solicitudes;

    @Test
    void solicitudSeEvaluaEnSegundoPlanoYElReporteReflejaLaCartera() throws Exception {
        // 1. Cliente sólido registrado por el servicio.
        Cliente cliente = clientes.registrar(new ComandoRegistrarCliente.Individual(
                "María López", "2547896320101", "maria@example.com",
                HistorialCrediticio.BUENO, new BigDecimal("8000.00"), TipoEmpleo.FORMAL, 6));

        // 2. POST /api/solicitudes responde 202 con la solicitud en Borrador.
        String respuesta = mvc.perform(post("/api/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": "%s",
                                  "tipoPrestamo": "PERSONAL",
                                  "monto": 50000.00,
                                  "plazoMeses": 60,
                                  "tasaAnual": 0.12
                                }
                                """.formatted(cliente.getId())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.estado").value("Borrador"))
                .andReturn().getResponse().getContentAsString();
        UUID prestamoId = UUID.fromString(json.readTree(respuesta).get("id").asText());

        // 3. El listener evalúa en background: se sondea hasta el estado final.
        EstadoPrestamo estadoFinal = esperarEvaluacion(prestamoId);
        assertThat(estadoFinal).isInstanceOf(EstadoPrestamo.Aprobado.class);
        assertThat(((EstadoPrestamo.Aprobado) estadoFinal).scoreObtenido())
                .isGreaterThanOrEqualTo(60);

        // 4. Desembolso del préstamo aprobado (publica EventoPrestamoDesembolsado).
        mvc.perform(post("/api/prestamos/{id}/desembolsar", prestamoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Desembolsado"));

        // 5. El reporte de cartera refleja los préstamos persistidos.
        mvc.perform(get("/api/reportes/cartera"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrestamos").value(1))
                .andExpect(jsonPath("$.montoTotal").value(50000.00))
                .andExpect(jsonPath("$.conteoPorRiesgo.MEDIO").value(1))
                .andExpect(jsonPath("$.indiceMora").value(0.0))
                .andExpect(jsonPath("$.conclusiones").isNotEmpty());
    }

    /** Sondea hasta que la evaluación asíncrona deje la solicitud en su estado final. */
    private EstadoPrestamo esperarEvaluacion(UUID prestamoId) throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MAXIMA_MS;
        while (System.currentTimeMillis() < limite) {
            EstadoPrestamo estado = solicitudes.buscarPorId(prestamoId).getEstado();
            if (estado instanceof EstadoPrestamo.Aprobado
                    || estado instanceof EstadoPrestamo.Rechazado) {
                return estado;
            }
            Thread.sleep(INTERVALO_SONDEO_MS);
        }
        throw new AssertionError("La evaluación asíncrona no terminó en "
                + ESPERA_MAXIMA_MS + " ms");
    }
}
