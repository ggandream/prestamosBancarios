package gt.edu.umg.prestamos.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidad de una cuota del plan de amortización. Cada préstamo tiene N cuotas
 * (relación {@code @OneToMany} desde {@code PrestamoEntity}). El plan es determinista:
 * se genera desde el dominio al persistir el préstamo y se guarda como snapshot para
 * consulta directa (endpoint plan-pagos de fases posteriores).
 */
@Entity
@Table(name = "cuota",
        uniqueConstraints = @UniqueConstraint(name = "uk_cuota_prestamo_numero",
                columnNames = {"prestamo_id", "numero"}))
public class CuotaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "prestamo_id", nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_cuota_prestamo"))
    private PrestamoEntity prestamo;

    @Column(name = "numero", nullable = false)
    private int numero;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Column(name = "capital", nullable = false, precision = 18, scale = 2)
    private BigDecimal capital;

    @Column(name = "interes", nullable = false, precision = 18, scale = 2)
    private BigDecimal interes;

    @Column(name = "total", nullable = false, precision = 18, scale = 2)
    private BigDecimal total;

    protected CuotaEntity() {
        // requerido por JPA
    }

    public CuotaEntity(int numero, LocalDate fechaPago, BigDecimal capital,
                       BigDecimal interes, BigDecimal total) {
        this.numero = numero;
        this.fechaPago = fechaPago;
        this.capital = capital;
        this.interes = interes;
        this.total = total;
    }

    void setPrestamo(PrestamoEntity prestamo) {
        this.prestamo = prestamo;
    }

    public Long getId() {
        return id;
    }

    public PrestamoEntity getPrestamo() {
        return prestamo;
    }

    public int getNumero() {
        return numero;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public BigDecimal getCapital() {
        return capital;
    }

    public BigDecimal getInteres() {
        return interes;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
