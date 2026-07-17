package gt.edu.umg.prestamos.api;

import gt.edu.umg.prestamos.config.SecurityConfig;
import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.cliente.ClienteIndividual;
import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.TipoEmpleo;
import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoPersonal;
import gt.edu.umg.prestamos.servicio.AmortizacionService;
import gt.edu.umg.prestamos.servicio.AmortizacionService.PlanAmortizacion;
import gt.edu.umg.prestamos.servicio.SolicitudService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests del slice web de {@link PrestamoController} con los servicios simulados. */
@WebMvcTest(PrestamoController.class)
@Import(SecurityConfig.class)
class PrestamoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SolicitudService solicitudes;

    @MockitoBean
    private AmortizacionService amortizacion;

    private static Cliente cliente() {
        return new ClienteIndividual(UUID.randomUUID(), "María López", "2547896320101",
                "maria@example.com", LocalDate.of(2020, 1, 15), HistorialCrediticio.BUENO,
                new BigDecimal("8000.00"), TipoEmpleo.FORMAL, 6);
    }

    private static Prestamo prestamo() {
        return new PrestamoPersonal(UUID.randomUUID(), cliente(),
                new BigDecimal("50000.00"), 60, new BigDecimal("0.12"),
                LocalDate.of(2026, 7, 16));
    }

    @Test
    void getPrestamosDevuelveLista() throws Exception {
        when(solicitudes.listar()).thenReturn(List.of(prestamo()));

        mvc.perform(get("/api/prestamos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("PERSONAL"))
                .andExpect(jsonPath("$[0].monto").value(50000.00));
    }

    @Test
    void getPlanPagosDevuelveCuotasYTotales() throws Exception {
        Prestamo prestamo = prestamo();
        when(amortizacion.generarPlan(prestamo.getId(), null)).thenReturn(
                new PlanAmortizacion(prestamo, "FRANCES", prestamo.generarPlanPagos()));

        mvc.perform(get("/api/prestamos/{id}/plan-pagos", prestamo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metodo").value("FRANCES"))
                .andExpect(jsonPath("$.cuotas.length()").value(60))
                .andExpect(jsonPath("$.totalIntereses").isNumber());
    }
}
