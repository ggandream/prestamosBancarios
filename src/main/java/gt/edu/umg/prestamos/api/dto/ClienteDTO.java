package gt.edu.umg.prestamos.api.dto;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.cliente.ClienteIndividual;
import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Representación pública de un cliente. Las entidades y objetos de dominio nunca
 * salen por la API; este record es lo único que ve el consumidor.
 */
public record ClienteDTO(
        UUID id,
        String tipo,
        String nombre,
        String documento,
        String email,
        LocalDate fechaRegistro,
        HistorialCrediticio historial,
        BigDecimal ingresoMensual,
        BigDecimal capacidadPago) {

    /** Mapea el cliente de dominio a su representación de API. */
    public static ClienteDTO desde(Cliente cliente) {
        String tipo = cliente instanceof ClienteIndividual ? "INDIVIDUAL" : "EMPRESARIAL";
        return new ClienteDTO(
                cliente.getId(), tipo, cliente.getNombre(), cliente.getDocumento(),
                cliente.getEmail(), cliente.getFechaRegistro(), cliente.getHistorial(),
                cliente.getIngresoMensual(), cliente.getCapacidadPago());
    }
}
