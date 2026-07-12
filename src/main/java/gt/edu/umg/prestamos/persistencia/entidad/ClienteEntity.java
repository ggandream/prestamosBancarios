package gt.edu.umg.prestamos.persistencia.entidad;

import gt.edu.umg.prestamos.dominio.cliente.HistorialCrediticio;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad JPA raíz de la jerarquía de clientes. Separada del dominio (Fase 2): los
 * mappers de {@code persistencia.mapper} traducen entre {@code Cliente} y esta entidad.
 *
 * <p>Estrategia de herencia <strong>JOINED</strong>: una tabla base {@code cliente} con
 * los atributos comunes y una tabla por subtipo con FK al id base. Se eligió sobre
 * {@code SINGLE_TABLE} porque permite declarar {@code NOT NULL} a nivel de columna en los
 * campos propios de cada subtipo (imposible con tabla única, donde deben ser nullables),
 * cumpliendo el requisito de restricciones de integridad de la Fase 2.
 */
@Entity
@Table(name = "cliente")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo_cliente")
public abstract class ClienteEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "documento", nullable = false, unique = true, length = 50)
    private String documento;

    @Column(name = "email", nullable = false, length = 200)
    private String email;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "historial", nullable = false, length = 20)
    private HistorialCrediticio historial;

    protected ClienteEntity() {
        // requerido por JPA
    }

    protected ClienteEntity(UUID id, String nombre, String documento, String email,
                            LocalDate fechaRegistro, HistorialCrediticio historial) {
        this.id = id;
        this.nombre = nombre;
        this.documento = documento;
        this.email = email;
        this.fechaRegistro = fechaRegistro;
        this.historial = historial;
    }

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
