package gt.edu.umg.prestamos.dominio.prestamo;

import gt.edu.umg.prestamos.dominio.calculo.CalculadoraInteres;
import gt.edu.umg.prestamos.dominio.cliente.Cliente;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Aprobado;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Borrador;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Desembolsado;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.EnEvaluacion;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.EnMora;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Pagado;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Rechazado;
import gt.edu.umg.prestamos.dominio.estado.TransicionInvalidaException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Raíz de la jerarquía de préstamos. Java puro.
 *
 * <p>Concentra el estado común, las validaciones, la máquina de estados
 * ({@link #cambiarEstado(EstadoPrestamo)}) y la generación del plan de pagos. El
 * algoritmo de amortización es un {@link CalculadoraInteres} (Strategy); cada subtipo
 * define su estrategia por defecto mediante {@link #calculadoraPorDefecto()}, de modo
 * que {@link #calcularCuota()} y {@link #calcularInteres()} quedan resueltos de forma
 * polimórfica sin duplicar aritmética.
 */
public abstract class Prestamo {

    private static final int ESCALA = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;
    private static final int PLAZO_MIN = 6;
    private static final int PLAZO_MAX = 360;

    private final UUID id;
    private final Cliente cliente;
    private final BigDecimal monto;
    private final int plazoMeses;
    private final BigDecimal tasaAnual;
    private final LocalDate fechaSolicitud;
    private EstadoPrestamo estado;

    protected Prestamo(UUID id, Cliente cliente, BigDecimal monto, int plazoMeses,
                       BigDecimal tasaAnual, LocalDate fechaSolicitud) {
        this.id = Objects.requireNonNull(id, "id no puede ser null");
        this.cliente = Objects.requireNonNull(cliente, "cliente no puede ser null");
        this.monto = exigirPositivo(monto, "monto");
        if (plazoMeses < PLAZO_MIN || plazoMeses > PLAZO_MAX) {
            throw new IllegalArgumentException(
                    "plazoMeses debe estar entre " + PLAZO_MIN + " y " + PLAZO_MAX);
        }
        this.plazoMeses = plazoMeses;
        this.tasaAnual = exigirPositivo(tasaAnual, "tasaAnual");
        this.fechaSolicitud = Objects.requireNonNull(fechaSolicitud, "fechaSolicitud no puede ser null");
        this.estado = new Borrador(LocalDateTime.now());
    }

    /** Estrategia de amortización por defecto del producto (Strategy hook polimórfico). */
    public abstract CalculadoraInteres calculadoraPorDefecto();

    /** Cuota representativa (la primera) según la estrategia por defecto del producto. */
    public BigDecimal calcularCuota() {
        return generarPlanPagos().getFirst().total();
    }

    /** Interés total del préstamo según la estrategia por defecto del producto. */
    public BigDecimal calcularInteres() {
        return generarPlanPagos().stream()
                .map(Cuota::interes)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(ESCALA, REDONDEO);
    }

    /** Plan de pagos con la estrategia por defecto del producto. */
    public List<Cuota> generarPlanPagos() {
        return generarPlanPagos(calculadoraPorDefecto());
    }

    /** Plan de pagos con una estrategia de cálculo explícita (inversión de dependencia). */
    public List<Cuota> generarPlanPagos(CalculadoraInteres calculadora) {
        return calculadora.calcular(monto, plazoMeses, tasaAnual, fechaSolicitud.plusMonths(1));
    }

    /**
     * Cambia el estado del préstamo validando que la transición sea legal.
     *
     * @throws TransicionInvalidaException si la transición no está permitida
     */
    public void cambiarEstado(EstadoPrestamo nuevo) {
        Objects.requireNonNull(nuevo, "nuevo estado no puede ser null");
        if (!transicionValida(estado, nuevo)) {
            throw new TransicionInvalidaException(
                    "Transición inválida: " + estado.getClass().getSimpleName()
                            + " -> " + nuevo.getClass().getSimpleName());
        }
        this.estado = nuevo;
    }

    /**
     * Restaura el estado persistido del agregado <strong>sin validar la transición</strong>.
     *
     * <p>Uso exclusivo de la capa de persistencia (Fase 2) al rehidratar un préstamo leído
     * de la base de datos: el estado guardado ya es un estado válido alcanzado en el pasado,
     * por lo que reproducir la máquina de estados con {@link #cambiarEstado(EstadoPrestamo)}
     * sería incorrecto (rechazaría, p. ej., reconstruir directamente un {@code EnMora}).
     * No debe usarse desde la lógica de negocio; para cambios de estado en runtime usar
     * {@link #cambiarEstado(EstadoPrestamo)}.
     */
    public void restaurarEstado(EstadoPrestamo estado) {
        this.estado = Objects.requireNonNull(estado, "estado no puede ser null");
    }

    private static boolean transicionValida(EstadoPrestamo actual, EstadoPrestamo nuevo) {
        return switch (actual) {
            case Borrador b -> nuevo instanceof EnEvaluacion;
            case EnEvaluacion e -> nuevo instanceof Aprobado || nuevo instanceof Rechazado;
            case Aprobado a -> nuevo instanceof Desembolsado;
            case Desembolsado d -> nuevo instanceof Pagado || nuevo instanceof EnMora;
            case EnMora m -> nuevo instanceof Pagado;
            case Rechazado r -> false;
            case Pagado p -> false;
        };
    }

    /** Descripción legible del estado actual (switch exhaustivo con pattern matching). */
    public String descripcionEstado() {
        return switch (estado) {
            case Borrador b -> "Borrador creado el " + b.fechaCreacion();
            case EnEvaluacion e -> "En evaluación por " + e.evaluador();
            case Aprobado a -> "Aprobado con score " + a.scoreObtenido();
            case Rechazado r -> "Rechazado: " + r.motivo();
            case Desembolsado d -> "Desembolsado por " + d.montoDesembolsado();
            case Pagado p -> "Pagado el " + p.fechaUltimoPago();
            case EnMora m -> "En mora: " + m.diasAtraso() + " días, vencido " + m.montoVencido();
        };
    }

    protected static BigDecimal exigirPositivo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException(campo + " debe ser positivo");
        }
        return valor;
    }

    // --- getters ---------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public int getPlazoMeses() {
        return plazoMeses;
    }

    public BigDecimal getTasaAnual() {
        return tasaAnual;
    }

    public LocalDate getFechaSolicitud() {
        return fechaSolicitud;
    }

    public EstadoPrestamo getEstado() {
        return estado;
    }
}
