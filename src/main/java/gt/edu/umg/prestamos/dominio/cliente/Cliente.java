package gt.edu.umg.prestamos.dominio.cliente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Raíz de la jerarquía de clientes. Java puro (sin dependencias de framework).
 *
 * <p>Define el estado y las validaciones comunes, un método concreto reutilizable
 * ({@link #calcularScoreBase()}) y contratos abstractos que cada subtipo resuelve
 * de forma polimórfica ({@link #getCapacidadPago()}, {@link #getIngresoMensual()},
 * {@link #getAntiguedadAnios()}).
 */
public abstract class Cliente {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UUID id;
    private final String nombre;
    private final String documento;
    private final String email;
    private final LocalDate fechaRegistro;
    private final HistorialCrediticio historial;

    protected Cliente(UUID id, String nombre, String documento, String email,
                      LocalDate fechaRegistro, HistorialCrediticio historial) {
        this.id = Objects.requireNonNull(id, "id no puede ser null");
        this.nombre = exigirNoVacio(nombre, "nombre");
        this.documento = exigirNoVacio(documento, "documento");
        this.email = exigirEmail(email);
        this.fechaRegistro = Objects.requireNonNull(fechaRegistro, "fechaRegistro no puede ser null");
        this.historial = Objects.requireNonNull(historial, "historial no puede ser null");
    }

    /**
     * Capacidad de pago mensual del cliente (monto máximo comprometible en cuota).
     * Cada subtipo define su fórmula.
     */
    public abstract BigDecimal getCapacidadPago();

    /** Ingreso mensual estimado del cliente. Base para {@code ReglaIngreso}. */
    public abstract BigDecimal getIngresoMensual();

    /**
     * Antigüedad en años del vínculo relevante para el riesgo (antigüedad laboral en
     * el individual, antigüedad del NIT en el empresarial). Base para {@code ReglaEdad}.
     */
    public abstract int getAntiguedadAnios();

    /**
     * Score base reutilizable derivado de la antigüedad del cliente en la institución.
     * Método concreto compartido por toda la jerarquía. Delega en la variante con fecha
     * de referencia para ser determinista y testeable.
     */
    public int calcularScoreBase() {
        return calcularScoreBase(LocalDate.now());
    }

    /** Variante determinista: calcula el score base contra una fecha de referencia dada. */
    public int calcularScoreBase(LocalDate fechaReferencia) {
        int anios = Period.between(fechaRegistro, fechaReferencia).getYears();
        if (anios >= 5) return 100;
        if (anios >= 3) return 80;
        if (anios >= 1) return 60;
        return 40;
    }

    // --- helpers de validación -------------------------------------------------

    protected static String exigirNoVacio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede estar vacío");
        }
        return valor;
    }

    protected static BigDecimal exigirPositivo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException(campo + " debe ser positivo");
        }
        return valor;
    }

    private static String exigirEmail(String email) {
        if (email == null || !EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("email con formato inválido: " + email);
        }
        return email;
    }

    // --- getters ---------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDocumento() {
        return documento;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public HistorialCrediticio getHistorial() {
        return historial;
    }
}
