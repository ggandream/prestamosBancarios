package gt.edu.umg.prestamos.persistencia.mapper;

import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.cliente.ClienteEmpresarial;
import gt.edu.umg.prestamos.dominio.cliente.ClienteIndividual;
import gt.edu.umg.prestamos.persistencia.entidad.ClienteEmpresarialEntity;
import gt.edu.umg.prestamos.persistencia.entidad.ClienteEntity;
import gt.edu.umg.prestamos.persistencia.entidad.ClienteIndividualEntity;
import org.springframework.stereotype.Component;

/**
 * Mapeador manual {@code Cliente} (dominio) &lt;-&gt; {@code ClienteEntity} (JPA).
 *
 * <p>La jerarquía de dominio no es sellada, por lo que los {@code switch} incluyen una
 * rama {@code default} defensiva: si apareciera un subtipo no soportado, se falla rápido
 * en lugar de persistir/leer datos incompletos.
 */
@Component
public class ClienteMapper {

    public ClienteEntity aEntidad(Cliente cliente) {
        return switch (cliente) {
            case ClienteIndividual ci -> new ClienteIndividualEntity(
                    ci.getId(), ci.getNombre(), ci.getDocumento(), ci.getEmail(),
                    ci.getFechaRegistro(), ci.getHistorial(),
                    ci.getSalarioMensual(), ci.getTipoEmpleo(), ci.getAntiguedadLaboral());
            case ClienteEmpresarial ce -> new ClienteEmpresarialEntity(
                    ce.getId(), ce.getNombre(), ce.getDocumento(), ce.getEmail(),
                    ce.getFechaRegistro(), ce.getHistorial(),
                    ce.getFacturacionAnual(), ce.getNit(), ce.getSector(), ce.getAntiguedadNit());
            default -> throw new IllegalArgumentException(
                    "Subtipo de Cliente no soportado: " + cliente.getClass().getName());
        };
    }

    public Cliente aDominio(ClienteEntity entity) {
        return switch (entity) {
            case ClienteIndividualEntity ci -> new ClienteIndividual(
                    ci.getId(), ci.getNombre(), ci.getDocumento(), ci.getEmail(),
                    ci.getFechaRegistro(), ci.getHistorial(),
                    ci.getSalarioMensual(), ci.getTipoEmpleo(), ci.getAntiguedadLaboral());
            case ClienteEmpresarialEntity ce -> new ClienteEmpresarial(
                    ce.getId(), ce.getNombre(), ce.getDocumento(), ce.getEmail(),
                    ce.getFechaRegistro(), ce.getHistorial(),
                    ce.getFacturacionAnual(), ce.getNit(), ce.getSector(), ce.getAntiguedadNit());
            default -> throw new IllegalArgumentException(
                    "Subtipo de ClienteEntity no soportado: " + entity.getClass().getName());
        };
    }
}
