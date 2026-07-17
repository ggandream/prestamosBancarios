package gt.edu.umg.prestamos.api;

import gt.edu.umg.prestamos.api.dto.PlanPagosDTO;
import gt.edu.umg.prestamos.api.dto.RespuestaPrestamoDTO;
import gt.edu.umg.prestamos.servicio.AmortizacionService;
import gt.edu.umg.prestamos.servicio.SolicitudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints de consulta de préstamos y de su plan de amortización. Sin lógica de
 * negocio.
 */
@RestController
@RequestMapping("/api/prestamos")
@Tag(name = "Préstamos", description = "Consulta de préstamos y planes de pago")
public class PrestamoController {

    private final SolicitudService solicitudes;
    private final AmortizacionService amortizacion;

    public PrestamoController(SolicitudService solicitudes, AmortizacionService amortizacion) {
        this.solicitudes = solicitudes;
        this.amortizacion = amortizacion;
    }

    @GetMapping
    @Operation(summary = "Lista todos los préstamos")
    public List<RespuestaPrestamoDTO> listar() {
        return solicitudes.listar().stream().map(RespuestaPrestamoDTO::desde).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta un préstamo por id")
    public RespuestaPrestamoDTO buscar(@PathVariable UUID id) {
        return RespuestaPrestamoDTO.desde(solicitudes.buscarPorId(id));
    }

    @GetMapping("/{id}/plan-pagos")
    @Operation(summary = "Genera el plan de amortización (FRANCES/ALEMAN; por defecto el del producto)")
    public PlanPagosDTO planPagos(@PathVariable UUID id,
                                  @RequestParam(required = false) String metodo) {
        return PlanPagosDTO.desde(amortizacion.generarPlan(id, metodo));
    }
}
