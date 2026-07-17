package gt.edu.umg.prestamos.servicio.comando;

import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.SectorIndustria;
import gt.edu.umg.prestamos.dominio.cliente.TipoEmpleo;

import java.math.BigDecimal;

/**
 * Comando de registro de cliente. Interfaz sellada: cada variante lleva exactamente
 * los datos de su tipo de cliente, de modo que {@code ClienteService} resuelve el
 * subtipo con un switch exhaustivo (sin campos nulos "de relleno").
 */
public sealed interface ComandoRegistrarCliente {

    String nombre();

    String documento();

    String email();

    HistorialCrediticio historial();

    /** Datos para registrar un {@code ClienteIndividual}. */
    record Individual(String nombre, String documento, String email, HistorialCrediticio historial,
                      BigDecimal salarioMensual, TipoEmpleo tipoEmpleo, int antiguedadLaboral)
            implements ComandoRegistrarCliente {}

    /** Datos para registrar un {@code ClienteEmpresarial}. */
    record Empresarial(String nombre, String documento, String email, HistorialCrediticio historial,
                       BigDecimal facturacionAnual, String nit, SectorIndustria sector, int antiguedadNit)
            implements ComandoRegistrarCliente {}
}
