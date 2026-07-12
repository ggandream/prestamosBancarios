package gt.edu.umg.prestamos.persistencia.entidad;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Préstamo de consumo. No agrega columnas propias; existe como tabla del árbol JOINED
 * para diferenciar el subtipo. Amortización francesa (definida en el dominio).
 */
@Entity
@Table(name = "prestamo_personal")
@DiscriminatorValue("PERSONAL")
@PrimaryKeyJoinColumn(name = "id")
public class PrestamoPersonalEntity extends PrestamoEntity {

    protected PrestamoPersonalEntity() {
        // requerido por JPA
    }

    public PrestamoPersonalEntity(UUID id, ClienteEntity cliente, BigDecimal monto, int plazoMeses,
                                  BigDecimal tasaAnual, LocalDate fechaSolicitud, EstadoEmbeddable estado) {
        super(id, cliente, monto, plazoMeses, tasaAnual, fechaSolicitud, estado);
    }
}
