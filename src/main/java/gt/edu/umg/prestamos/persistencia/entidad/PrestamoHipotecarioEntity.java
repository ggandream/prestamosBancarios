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
 * Préstamo hipotecario. Agrega la garantía y el avalúo del inmueble, ambos
 * {@code NOT NULL} en la tabla {@code prestamo_hipotecario}.
 */
@Entity
@Table(name = "prestamo_hipotecario")
@DiscriminatorValue("HIPOTECARIO")
@PrimaryKeyJoinColumn(name = "id")
public class PrestamoHipotecarioEntity extends PrestamoEntity {

    @Column(name = "descripcion_garantia", nullable = false, length = 500)
    private String descripcionGarantia;

    @Column(name = "avaluo", nullable = false, precision = 18, scale = 2)
    private BigDecimal avaluo;

    protected PrestamoHipotecarioEntity() {
        // requerido por JPA
    }

    public PrestamoHipotecarioEntity(UUID id, ClienteEntity cliente, BigDecimal monto, int plazoMeses,
                                     BigDecimal tasaAnual, LocalDate fechaSolicitud, EstadoEmbeddable estado,
                                     String descripcionGarantia, BigDecimal avaluo) {
        super(id, cliente, monto, plazoMeses, tasaAnual, fechaSolicitud, estado);
        this.descripcionGarantia = descripcionGarantia;
        this.avaluo = avaluo;
    }

    public String getDescripcionGarantia() {
        return descripcionGarantia;
    }

    public BigDecimal getAvaluo() {
        return avaluo;
    }
}
