package gt.edu.umg.prestamos.persistencia.entidad;

import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import gt.edu.umg.prestamos.dominio.cliente.TipoEmpleo;
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
 * Entidad del cliente persona natural. Tabla {@code cliente_individual} unida por FK a
 * {@code cliente} (estrategia JOINED). Sus campos propios son {@code NOT NULL}.
 */
@Entity
@Table(name = "cliente_individual")
@DiscriminatorValue("INDIVIDUAL")
@PrimaryKeyJoinColumn(name = "id")
public class ClienteIndividualEntity extends ClienteEntity {

    @Column(name = "salario_mensual", nullable = false, precision = 18, scale = 2)
    private BigDecimal salarioMensual;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_empleo", nullable = false, length = 20)
    private TipoEmpleo tipoEmpleo;

    @Column(name = "antiguedad_laboral", nullable = false)
    private int antiguedadLaboral;

    protected ClienteIndividualEntity() {
        // requerido por JPA
    }

    public ClienteIndividualEntity(UUID id, String nombre, String documento, String email,
                                   LocalDate fechaRegistro, HistorialCrediticio historial,
                                   BigDecimal salarioMensual, TipoEmpleo tipoEmpleo, int antiguedadLaboral) {
        super(id, nombre, documento, email, fechaRegistro, historial);
        this.salarioMensual = salarioMensual;
        this.tipoEmpleo = tipoEmpleo;
        this.antiguedadLaboral = antiguedadLaboral;
    }

    public BigDecimal getSalarioMensual() {
        return salarioMensual;
    }

    public TipoEmpleo getTipoEmpleo() {
        return tipoEmpleo;
    }

    public int getAntiguedadLaboral() {
        return antiguedadLaboral;
    }
}
