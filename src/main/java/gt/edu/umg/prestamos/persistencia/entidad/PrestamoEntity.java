package gt.edu.umg.prestamos.persistencia.entidad;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad JPA raíz de la jerarquía de préstamos (estrategia JOINED, por las mismas
 * razones de integridad que {@code ClienteEntity}). Referencia al cliente por FK,
 * embebe el estado ({@link EstadoEmbeddable}) y agrega las cuotas del plan de pagos.
 */
@Entity
@Table(name = "prestamo")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo_prestamo")
public abstract class PrestamoEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_prestamo_cliente"))
    private ClienteEntity cliente;

    @Column(name = "monto", nullable = false, precision = 18, scale = 2)
    private BigDecimal monto;

    @Column(name = "plazo_meses", nullable = false)
    private int plazoMeses;

    @Column(name = "tasa_anual", nullable = false, precision = 10, scale = 6)
    private BigDecimal tasaAnual;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDate fechaSolicitud;

    @Embedded
    private EstadoEmbeddable estado;

    @OneToMany(mappedBy = "prestamo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<CuotaEntity> cuotas = new ArrayList<>();

    protected PrestamoEntity() {
        // requerido por JPA
    }

    protected PrestamoEntity(UUID id, ClienteEntity cliente, BigDecimal monto, int plazoMeses,
                             BigDecimal tasaAnual, LocalDate fechaSolicitud, EstadoEmbeddable estado) {
        this.id = id;
        this.cliente = cliente;
        this.monto = monto;
        this.plazoMeses = plazoMeses;
        this.tasaAnual = tasaAnual;
        this.fechaSolicitud = fechaSolicitud;
        this.estado = estado;
    }

    /** Agrega una cuota manteniendo la relación bidireccional consistente. */
    public void agregarCuota(CuotaEntity cuota) {
        cuota.setPrestamo(this);
        this.cuotas.add(cuota);
    }

    public UUID getId() {
        return id;
    }

    public ClienteEntity getCliente() {
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

    public EstadoEmbeddable getEstado() {
        return estado;
    }

    public void setEstado(EstadoEmbeddable estado) {
        this.estado = estado;
    }

    public List<CuotaEntity> getCuotas() {
        return cuotas;
    }
}
