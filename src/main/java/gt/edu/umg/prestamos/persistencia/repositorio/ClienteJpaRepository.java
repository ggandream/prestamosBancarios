package gt.edu.umg.prestamos.persistencia.repositorio;

import gt.edu.umg.prestamos.persistencia.entidad.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data sobre {@link ClienteEntity}. Trabaja con entidades JPA; el
 * {@code ClienteRepositorioJpa} lo envuelve para exponer objetos de dominio.
 */
public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, UUID> {

    Optional<ClienteEntity> findByDocumento(String documento);

    boolean existsByDocumento(String documento);
}
