package gt.edu.umg.prestamos.persistencia.repositorio;

import gt.edu.umg.prestamos.persistencia.entidad.PrestamoEntity;
import gt.edu.umg.prestamos.persistencia.entidad.TipoEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data sobre {@link PrestamoEntity}. Cubre las consultas de la Fase 2:
 * por cliente, por estado y por tipo de préstamo.
 */
public interface PrestamoJpaRepository extends JpaRepository<PrestamoEntity, UUID> {

    /** Préstamos de un cliente (deriva a {@code where cliente_id = ?}). */
    List<PrestamoEntity> findByClienteId(UUID clienteId);

    /** Préstamos en un estado dado (deriva sobre el campo embebido {@code estado.tipo}). */
    List<PrestamoEntity> findByEstadoTipo(TipoEstado tipo);

    /**
     * Préstamos de un subtipo concreto, discriminando por el tipo de entidad de la
     * jerarquía JOINED (p. ej. {@code PrestamoHipotecarioEntity.class}).
     */
    @Query("select p from PrestamoEntity p where type(p) = :tipo")
    List<PrestamoEntity> findByTipo(@Param("tipo") Class<? extends PrestamoEntity> tipo);
}
