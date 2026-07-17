package gt.edu.umg.prestamos.api;

import gt.edu.umg.prestamos.api.dto.ClienteDTO;
import gt.edu.umg.prestamos.api.dto.RegistroClienteDTO;
import gt.edu.umg.prestamos.servicio.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints de clientes. Sin lógica de negocio: valida formato, delega al servicio y
 * mapea dominio → DTO.
 */
@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "Registro y consulta de clientes")
public class ClienteController {

    private final ClienteService servicio;

    public ClienteController(ClienteService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un cliente (INDIVIDUAL o EMPRESARIAL)")
    public ClienteDTO registrar(@Valid @RequestBody RegistroClienteDTO solicitud) {
        return ClienteDTO.desde(servicio.registrar(solicitud.aComando()));
    }

    @GetMapping
    @Operation(summary = "Lista todos los clientes")
    public List<ClienteDTO> listar() {
        return servicio.listar().stream().map(ClienteDTO::desde).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta un cliente por id")
    public ClienteDTO buscar(@PathVariable UUID id) {
        return ClienteDTO.desde(servicio.buscarPorId(id));
    }
}
