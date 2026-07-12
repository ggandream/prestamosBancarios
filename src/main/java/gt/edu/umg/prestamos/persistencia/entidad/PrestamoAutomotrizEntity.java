package gt.edu.umg.prestamos.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Préstamo automotriz. Agrega el vehículo financiado y su depreciación anual, ambos
 * {@code NOT NULL} en la tabla {@code prestamo_automotriz}. Amortización alemana.
 */
@Entity
@Table(name = "prestamo_automotriz")
@DiscriminatorValue("AUTOMOTRIZ")
@PrimaryKeyJoinColumn(name = "id")
public class PrestamoAutomotrizEntity extends PrestamoEntity {

    @Column(name = "vehiculo", nullable = false, length = 200)
    private String vehiculo;

    @Column(name = "depreciacion_anual", nullable = false, precision = 18, scale = 2)
    private BigDecimal depreciacionAnual;

    protected PrestamoAutomotrizEntity() {
        // requerido por JPA
    }

    public PrestamoAutomotrizEntity(UUID id, ClienteEntity cliente, BigDecimal monto, int plazoMeses,
                                    BigDecimal tasaAnual, LocalDate fechaSolicitud, EstadoEmbeddable estado,
                                    String vehiculo, BigDecimal depreciacionAnual) {
        super(id, cliente, monto, plazoMeses, tasaAnual, fechaSolicitud, estado);
        this.vehiculo = vehiculo;
        this.depreciacionAnual = depreciacionAnual;
    }

    public String getVehiculo() {
        return vehiculo;
    }

    public BigDecimal getDepreciacionAnual() {
        return depreciacionAnual;
    }
}
