package gt.edu.umg.prestamos.api;

import gt.edu.umg.prestamos.api.dto.RespuestaPrestamoDTO;
import gt.edu.umg.prestamos.api.dto.ResultadoEvaluacionDTO;
import gt.edu.umg.prestamos.api.dto.SolicitudPrestamoDTO;
import gt.edu.umg.prestamos.servicio.EvaluacionService;
import gt.edu.umg.prestamos.servicio.SolicitudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoints de solicitudes de préstamo: creación en Borrador y evaluación con el
 * motor de scoring. Sin lógica de negocio.
 */
@RestController
@RequestMapping("/api/solicitudes")
@Tag(name = "Solicitudes", description = "Creación y evaluación de solicitudes de préstamo")
public class SolicitudController {

    private final SolicitudService solicitudes;
    private final EvaluacionService evaluacion;

    public SolicitudController(SolicitudService solicitudes, EvaluacionService evaluacion) {
        this.solicitudes = solicitudes;
        this.evaluacion = evaluacion;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una solicitud de préstamo en estado Borrador")
    public RespuestaPrestamoDTO crear(@Valid @RequestBody SolicitudPrestamoDTO solicitud) {
        return RespuestaPrestamoDTO.desde(solicitudes.crear(solicitud.aComando()));
    }

    @PostMapping("/{id}/evaluar")
    @Operation(summary = "Evalúa una solicitud con el motor de scoring (Aprobado/Rechazado)")
    public ResultadoEvaluacionDTO evaluar(@PathVariable UUID id) {
        return ResultadoEvaluacionDTO.desde(evaluacion.evaluar(id));
    }
}
