package gt.edu.umg.prestamos.api;

import gt.edu.umg.prestamos.api.dto.ResumenCarteraDTO;
import gt.edu.umg.prestamos.servicio.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de reportería. Sin lógica de negocio. */
@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes", description = "Análisis de la cartera de préstamos")
public class ReporteController {

    private final ReporteService servicio;

    public ReporteController(ReporteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/cartera")
    @Operation(summary = "Resumen de la cartera: totales, índice de mora, agrupaciones y conclusiones")
    public ResumenCarteraDTO cartera() {
        return ResumenCarteraDTO.desde(servicio.resumenCartera());
    }
}
