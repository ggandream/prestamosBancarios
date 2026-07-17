package gt.edu.umg.prestamos.api;

import gt.edu.umg.prestamos.config.SecurityConfig;
import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.cliente.ClienteIndividual;
import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.TipoEmpleo;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo;
import gt.edu.umg.prestamos.dominio.estado.TransicionInvalidaException;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoPersonal;
import gt.edu.umg.prestamos.dominio.scoring.ResultadoEvaluacion;
import gt.edu.umg.prestamos.servicio.EvaluacionService;
import gt.edu.umg.prestamos.servicio.EvaluacionService.EvaluacionCompletada;
import gt.edu.umg.prestamos.servicio.SolicitudService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests del slice web de {@link SolicitudController} con los servicios simulados. */
@WebMvcTest(SolicitudController.class)
@Import(SecurityConfig.class)
class SolicitudControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SolicitudService solicitudes;

    @MockitoBean
    private EvaluacionService evaluacion;

    private static Cliente cliente() {
        return new ClienteIndividual(UUID.randomUUID(), "María López", "2547896320101",
                "maria@example.com", LocalDate.of(2020, 1, 15), HistorialCrediticio.BUENO,
                new BigDecimal("8000.00"), TipoEmpleo.FORMAL, 6);
    }

    private static Prestamo prestamo(Cliente cliente) {
        return new PrestamoPersonal(UUID.randomUUID(), cliente,
                new BigDecimal("50000.00"), 60, new BigDecimal("0.12"),
                LocalDate.of(2026, 7, 16));
    }

    @Test
    void postSolicitudValidaDevuelve201EnBorrador() throws Exception {
        Cliente cliente = cliente();
        when(solicitudes.crear(any())).thenReturn(prestamo(cliente));

        mvc.perform(post("/api/solicitudes")
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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("PERSONAL"))
                .andExpect(jsonPath("$.estado").value("Borrador"));
    }

    @Test
    void postSolicitudConPlazoInvalidoDevuelve400() throws Exception {
        mvc.perform(post("/api/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": "%s",
                                  "tipoPrestamo": "PERSONAL",
                                  "monto": 50000.00,
                                  "plazoMeses": 3,
                                  "tasaAnual": 0.12
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evaluarDevuelve200ConScoreYDecision() throws Exception {
        Prestamo prestamo = prestamo(cliente());
        prestamo.cambiarEstado(new EstadoPrestamo.EnEvaluacion(LocalDateTime.now(), "motor-scoring"));
        prestamo.cambiarEstado(new EstadoPrestamo.Aprobado(LocalDateTime.now(), 88));
        when(evaluacion.evaluar(prestamo.getId())).thenReturn(new EvaluacionCompletada(
                prestamo, new ResultadoEvaluacion(88, "APROBADO", List.of("detalle"))));

        mvc.perform(post("/api/solicitudes/{id}/evaluar", prestamo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(88))
                .andExpect(jsonPath("$.decision").value("APROBADO"))
                .andExpect(jsonPath("$.estado").value("Aprobado"));
    }

    @Test
    void evaluarSolicitudYaEvaluadaDevuelve409() throws Exception {
        UUID id = UUID.randomUUID();
        when(evaluacion.evaluar(id))
                .thenThrow(new TransicionInvalidaException("Transición inválida"));

        mvc.perform(post("/api/solicitudes/{id}/evaluar", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }
}
