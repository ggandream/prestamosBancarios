package gt.edu.umg.prestamos.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Estado del préstamo aplanado a columnas, embebido en {@code PrestamoEntity}.
 *
 * <p>Estrategia (Fase 2, punto 2): columna discriminadora {@code estado_tipo} +
 * columnas de datos genéricas y nullables que cubren, en conjunto, los campos de los
 * 7 records de {@code EstadoPrestamo}. El {@code EstadoMapper} decide qué columnas
 * pobla según el tipo y reconstruye el record correcto al leer:
 *
 * <ul>
 *   <li>{@code Borrador(fechaCreacion)} → fecha</li>
 *   <li>{@code EnEvaluacion(fechaInicio, evaluador)} → fecha, texto</li>
 *   <li>{@code Aprobado(fechaAprobacion, scoreObtenido)} → fecha, score</li>
 *   <li>{@code Rechazado(fechaRechazo, motivo)} → fecha, texto</li>
 *   <li>{@code Desembolsado(fecha, montoDesembolsado)} → fecha, monto</li>
 *   <li>{@code Pagado(fechaUltimoPago)} → fecha</li>
 *   <li>{@code EnMora(diasAtraso, montoVencido)} → diasAtraso, monto</li>
 * </ul>
 */
@Embeddable
public class EstadoEmbeddable {

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_tipo", nullable = false, length = 20)
    private TipoEstado tipo;

    @Column(name = "estado_fecha")
    private LocalDateTime fecha;

    @Column(name = "estado_texto", length = 500)
    private String texto;

    @Column(name = "estado_score")
    private Integer score;

    @Column(name = "estado_monto", precision = 18, scale = 2)
    private BigDecimal monto;

    @Column(name = "estado_dias_atraso")
    private Integer diasAtraso;

    protected EstadoEmbeddable() {
        // requerido por JPA
    }

    public EstadoEmbeddable(TipoEstado tipo, LocalDateTime fecha, String texto,
                            Integer score, BigDecimal monto, Integer diasAtraso) {
        this.tipo = tipo;
        this.fecha = fecha;
        this.texto = texto;
        this.score = score;
        this.monto = monto;
        this.diasAtraso = diasAtraso;
    }

    public TipoEstado getTipo() {
        return tipo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getTexto() {
        return texto;
    }

    public Integer getScore() {
        return score;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public Integer getDiasAtraso() {
        return diasAtraso;
    }
}
