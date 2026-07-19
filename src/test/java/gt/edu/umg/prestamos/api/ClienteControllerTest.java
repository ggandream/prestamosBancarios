package gt.edu.umg.prestamos.api;

import gt.edu.umg.prestamos.config.SecurityConfig;
import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.cliente.ClienteIndividual;
import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.TipoEmpleo;
import gt.edu.umg.prestamos.servicio.ClienteService;
import gt.edu.umg.prestamos.servicio.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests del slice web de {@link ClienteController} con el servicio simulado. */
@WebMvcTest(ClienteController.class)
@Import(SecurityConfig.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ClienteService servicio;

    private static Cliente clienteMaria() {
        return new ClienteIndividual(UUID.randomUUID(), "María López", "2547896320101",
                "maria@example.com", LocalDate.of(2020, 1, 15), HistorialCrediticio.BUENO,
                new BigDecimal("8000.00"), TipoEmpleo.FORMAL, 6);
    }

    @Test
    void postClienteValidoDevuelve201ConDto() throws Exception {
        when(servicio.registrar(any())).thenReturn(clienteMaria());

        mvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "INDIVIDUAL",
                                  "nombre": "María López",
                                  "documento": "2547896320101",
                                  "email": "maria@example.com",
                                  "historial": "BUENO",
                                  "salarioMensual": 8000.00,
                                  "tipoEmpleo": "FORMAL",
                                  "antiguedadLaboral": 6
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.nombre").value("María López"))
                .andExpect(jsonPath("$.capacidadPago").value(2800.00));
    }

    @Test
    void postClienteSinNombreDevuelve400ConDetalle() throws Exception {
        mvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "INDIVIDUAL",
                                  "documento": "2547896320101",
                                  "email": "maria@example.com",
                                  "historial": "BUENO"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").isNotEmpty());
    }

    @Test
    void getClientesDevuelveLista() throws Exception {
        when(servicio.listar()).thenReturn(List.of(clienteMaria()));

        mvc.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documento").value("2547896320101"));
    }

    @Test
    void getClienteInexistenteDevuelve404() throws Exception {
        UUID id = UUID.randomUUID();
        when(servicio.buscarPorId(id))
                .thenThrow(new RecursoNoEncontradoException("Cliente no encontrado: " + id));

        mvc.perform(get("/api/clientes/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
