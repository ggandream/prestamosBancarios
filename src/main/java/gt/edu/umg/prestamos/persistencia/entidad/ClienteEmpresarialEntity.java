package gt.edu.umg.prestamos.persistencia.entidad;

import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.SectorIndustria;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad del cliente persona jurídica. Tabla {@code cliente_empresarial} unida por FK a
 * {@code cliente} (estrategia JOINED). El {@code nit} es único y {@code NOT NULL}.
 */
@Entity
@Table(name = "cliente_empresarial")
@DiscriminatorValue("EMPRESARIAL")
@PrimaryKeyJoinColumn(name = "id")
public class ClienteEmpresarialEntity extends ClienteEntity {

    @Column(name = "facturacion_anual", nullable = false, precision = 18, scale = 2)
    private BigDecimal facturacionAnual;

    @Column(name = "nit", nullable = false, unique = true, length = 50)
    private String nit;

    @Enumerated(EnumType.STRING)
    @Column(name = "sector", nullable = false, length = 20)
    private SectorIndustria sector;

    @Column(name = "antiguedad_nit", nullable = false)
    private int antiguedadNit;

    protected ClienteEmpresarialEntity() {
        // requerido por JPA
    }

    public ClienteEmpresarialEntity(UUID id, String nombre, String documento, String email,
                                    LocalDate fechaRegistro, HistorialCrediticio historial,
                                    BigDecimal facturacionAnual, String nit, SectorIndustria sector,
                                    int antiguedadNit) {
        super(id, nombre, documento, email, fechaRegistro, historial);
        this.facturacionAnual = facturacionAnual;
        this.nit = nit;
        this.sector = sector;
        this.antiguedadNit = antiguedadNit;
    }

    public BigDecimal getFacturacionAnual() {
        return facturacionAnual;
    }

    public String getNit() {
        return nit;
    }

    public SectorIndustria getSector() {
        return sector;
    }

    public int getAntiguedadNit() {
        return antiguedadNit;
    }
}
