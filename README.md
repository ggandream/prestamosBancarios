# Plataforma de Gestión y Evaluación de Préstamos

Sistema de gestión y evaluación de solicitudes de préstamos para una institución financiera. Registra clientes, captura solicitudes, las evalúa mediante un motor de scoring basado en reglas, genera planes de amortización y procesa operaciones de forma asíncrona.

> Proyecto académico — Programación Avanzada en Java. El objetivo es demostrar dominio de Java moderno, SOLID, patrones de diseño y prácticas DevOps.

---

## Equipo

| Integrante | Rol |
|---|---|
| Andrea Garrido | Persistencia (JPA, modelo de datos, migraciones) |
| Luis Humberto Ruiz | Dev/Ops y seguridad (Docker, CI/CD, configuración) |
| Luis Renato Granados | Backend (servicios, scoring, API REST) |

---

## Requisitos

- Java 25+
- Docker y Docker Compose
- PostgreSQL 15+ (si se ejecuta sin Docker)
- Maven 3.9+

> El proyecto se probó con **Maven 3.9.6**. Con SDKMAN: `sdk use maven 3.9.6`.

---

## Comandos

Todos se ejecutan desde la raíz del proyecto.

| Comando | Qué hace |
|---|---|
| `mvn verify` | Compila y corre **toda** la suite de tests. Es el comando que debe pasar en verde antes de cada PR. |
| `mvn test` | Corre solo los tests unitarios. |
| `mvn compile` | Compila el código fuente principal. |
| `mvn clean` | Borra el directorio `target/`. |
| `mvn package` | Genera el JAR ejecutable en `target/prestamos-0.0.1-SNAPSHOT.jar`. |
| `mvn clean verify` | Build limpio de punta a punta (lo que corre el CI). |
| `mvn spring-boot:run` | Levanta la aplicación. |

Ejemplos útiles:

```bash
# Correr una sola clase de test
mvn test -Dtest=CalculadoraInteresTest

# Correr un solo método
mvn test -Dtest=MotorScoringTest#promedioEsPonderado
```

> **Nota sobre `spring-boot:run`:** el perfil por defecto es `dev` y espera un PostgreSQL en `localhost:5432`. Puedes levantarlo con `docker compose up -d db` (ver abajo). Los **tests no necesitan base de datos**: usan el perfil `test` con H2 en memoria.

---

## Base de datos y Docker (Fase 2)

> 📄 Documentación detallada de la capa de persistencia (modelo de datos, mapeo JPA,
> estados, migraciones y tests): **[docs/fase2-persistencia.md](docs/fase2-persistencia.md)**.

El proyecto trae un `docker-compose.yml` que levanta **PostgreSQL** y, opcionalmente, la **aplicación**.

```bash
# Solo la base de datos (para desarrollar la app en local con mvn spring-boot:run)
docker compose up -d db

# Toda la plataforma (app + db) desde cero
docker compose up --build

# Detener (conserva los datos en el volumen db-data)
docker compose down

# Detener y borrar también los datos
docker compose down -v
```

Con la app corriendo, la API queda en `http://localhost:8080` y **Swagger UI** en
`http://localhost:8080/swagger-ui.html`.

**Credenciales** (por variables de entorno, nunca hardcodeadas). Valores por defecto para
desarrollo; se pueden sobreescribir con un archivo `.env` en la raíz:

| Variable | Default | Uso |
|---|---|---|
| `DB_NAME` | `prestamos` | nombre de la base de datos |
| `DB_USER` | `prestamos` | usuario |
| `DB_PASSWORD` | `prestamos` | contraseña |
| `DB_PORT` | `5432` | puerto de PostgreSQL en el host |
| `APP_PORT` | `8080` | puerto de la app en el host |
| `PGADMIN_PORT` | `8082` | puerto de pgAdmin en el host |
| `PGADMIN_EMAIL` | `admin@admin.com` | usuario de acceso a pgAdmin (evitar dominios reservados como `.local`) |
| `PGADMIN_PASSWORD` | `admin123` | contraseña de acceso a pgAdmin (mínimo 6 caracteres) |

> El servicio `app` se construye con un `Dockerfile` multi-stage **starter**; Luis lo
> afinará en la Fase 5 (DevOps). El servicio `db` ya es de uso definitivo.

### Explorar la base de datos desde el navegador (pgAdmin)

PostgreSQL **no** se abre desde el navegador (habla un protocolo binario, no HTTP). Para
inspeccionar la BD de forma visual, el compose incluye **pgAdmin** (solo desarrollo):

1. Abre **`http://localhost:8082`**.
2. Inicia sesión con `admin@admin.com` / `admin123` (o lo que definas en las variables).
3. En el árbol de la izquierda ya aparece el servidor **`prestamos-db`** pre-registrado
   (grupo *Servidores*). Al expandirlo por primera vez te pedirá la contraseña de la BD:
   escribe **`prestamos`** (puedes marcar «guardar contraseña»).
4. Navega a *prestamos-db → Databases → prestamos → Schemas → public → Tables*.

Alternativa sin navegador (cliente `psql` dentro del contenedor):

```bash
docker exec -it prestamos-db psql -U prestamos -d prestamos -c "\dt"
```

### Esquema de datos

El esquema lo administra **Flyway** (`src/main/resources/db/migration/V1__esquema_inicial.sql`):
se aplica automáticamente al arrancar el perfil `dev`. Hibernate se limita a **validar**
(`ddl-auto: validate`) que las entidades JPA coincidan con el esquema migrado; nunca crea
ni altera tablas.

- **Herencia:** estrategia `JOINED` (tabla base + tabla por subtipo con FK) tanto para
  clientes como para préstamos — permite `NOT NULL` reales en los campos de cada subtipo.
- **Estado del préstamo:** columna discriminadora `estado_tipo` + columnas de datos; el
  `EstadoMapper` reconstruye el record exacto del sealed interface `EstadoPrestamo`.
- **Entidades JPA separadas del dominio:** viven en `persistencia.entidad` y se traducen con
  mapeadores manuales en `persistencia.mapper`; las entidades nunca salen de la capa de
  persistencia (los adaptadores en `persistencia.adaptador` exponen objetos de dominio).

---

## Motor de scoring

Evaluación 100% determinista basada en 4 reglas ponderadas:

| Regla | Peso |
|---|---|
| Capacidad de pago | 40% |
| Ingreso | 25% |
| Historial crediticio | 25% |
| Edad | 10% |

**Decisión:** score ≥ 60 → Aprobado · score < 60 → Rechazado.

---

## Flujo de estados

```
Borrador → EnEvaluacion → Aprobado → Desembolsado → Pagado
                        ↘ Rechazado
         (Desembolsado puede pasar a EnMora)
```

---

## Stack tecnológico

- **Java 25** · Spring Boot 3.x · Spring Data JPA
- **PostgreSQL** (producción) · H2 (tests)
- Eventos asíncronos nativos de Spring (`@Async`, `@EventListener`)
- springdoc-openapi · Jakarta Bean Validation
- JUnit 5 · Mockito · Testcontainers · JaCoCo
- Docker (multi-stage) · GitHub Actions

---

## CI/CD

GitHub Actions ejecuta en cada push:
1. Compilación
2. Tests (unitarios + integración)
3. Generación y publicación del reporte de cobertura JaCoCo

El build falla si algún test no pasa.
