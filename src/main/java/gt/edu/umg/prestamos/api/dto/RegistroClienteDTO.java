package gt.edu.umg.prestamos.api.dto;

import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.SectorIndustria;
import gt.edu.umg.prestamos.dominio.cliente.TipoEmpleo;
import gt.edu.umg.prestamos.servicio.comando.ComandoRegistrarCliente;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Request de registro de cliente. Es plano (un solo JSON con campo discriminador
 * {@code tipo}); {@link #aComando()} lo traduce al comando sellado de la capa de
 * servicio con solo los campos de la variante.
 *
 * @param tipo              INDIVIDUAL o EMPRESARIAL
 * @param historial         historial crediticio almacenado (BUENO/REGULAR/MALO)
 * @param salarioMensual    solo INDIVIDUAL
 * @param tipoEmpleo        solo INDIVIDUAL (FORMAL/INFORMAL/INDEPENDIENTE)
 * @param antiguedadLaboral solo INDIVIDUAL, en años
 * @param facturacionAnual  solo EMPRESARIAL
 * @param nit               solo EMPRESARIAL
 * @param sector            solo EMPRESARIAL
 * @param antiguedadNit     solo EMPRESARIAL, en años
 */
public record RegistroClienteDTO(
        @NotBlank @Pattern(regexp = "INDIVIDUAL|EMPRESARIAL") String tipo,
        @NotBlank String nombre,
        @NotBlank String documento,
        @NotBlank @Email String email,
        @NotNull HistorialCrediticio historial,
        @Positive BigDecimal salarioMensual,
        TipoEmpleo tipoEmpleo,
        @PositiveOrZero Integer antiguedadLaboral,
        @Positive BigDecimal facturacionAnual,
        String nit,
        SectorIndustria sector,
        @PositiveOrZero Integer antiguedadNit) {

    /** Traduce el request al comando de la capa de servicio. */
    public ComandoRegistrarCliente aComando() {
        return switch (tipo) {
            case "INDIVIDUAL" -> new ComandoRegistrarCliente.Individual(
                    nombre, documento, email, historial,
                    salarioMensual, tipoEmpleo, valorODefecto(antiguedadLaboral));
            case "EMPRESARIAL" -> new ComandoRegistrarCliente.Empresarial(
                    nombre, documento, email, historial,
                    facturacionAnual, nit, sector, valorODefecto(antiguedadNit));
            default -> throw new IllegalArgumentException("Tipo de cliente no soportado: " + tipo);
        };
    }

    private static int valorODefecto(Integer valor) {
        return valor == null ? 0 : valor;
    }
}
