package gt.edu.umg.prestamos.persistencia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifica que la migración Flyway y el mapeo JPA no se desincronicen (Fase 2).
 *
 * <p>A diferencia del resto de tests —que dejan a Hibernate generar el esquema desde las
 * entidades— aquí se activa Flyway para que aplique {@code V1__esquema_inicial.sql} y se
 * pone {@code ddl-auto=validate}: si el contexto arranca, es que <strong>Hibernate validó
 * cada entidad contra el esquema migrado</strong> (mismas tablas, columnas, tipos, escalas).
 * Es el mismo mecanismo del perfil {@code dev} contra PostgreSQL, ejecutado aquí sobre H2
 * en modo de compatibilidad PostgreSQL. Cualquier deriva (una columna renombrada, una
 * escala distinta, un tipo incompatible) rompe este test.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class MigracionEsquemaTest {

    @Test
    void migracionFlywayAplicaYHibernateValidaLasEntidades() {
        // El arranque del contexto es la aserción: Flyway migró y Hibernate validó sin errores.
    }
}
