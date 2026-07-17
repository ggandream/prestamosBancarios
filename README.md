# Plataforma de Gestión y Evaluación de Préstamos

Backend académico para registrar clientes, persistir préstamos, evaluar solicitudes de forma asíncrona mediante reglas de scoring, generar planes de amortización y reportar la cartera. Usa Java 25, Spring Boot, PostgreSQL, Flyway, Maven, Docker y GitHub Actions.

## Equipo

| Integrante | Responsabilidad |
|---|---|
| Andrea Garrido | Persistencia JPA, modelo de datos y migraciones |
| Luis Humberto Ruiz | DevOps, seguridad, Docker, CI/CD y configuración |
| Luis Renato Granados | Servicios, scoring y API REST |

## Requisitos

- Docker Desktop con Docker Compose v2 (recomendado).
- Para ejecución local sin Docker: JDK 25, Maven 3.9+ y PostgreSQL 16.

## Levantar todo con Docker Compose

1. Crear el archivo local de configuración. `.env` está ignorado por Git.

   PowerShell:

   ```powershell
   Copy-Item .env.example .env
   ```

   Linux/macOS:

   ```bash
   cp .env.example .env
   ```

2. Abrir `.env` y reemplazar todos los valores `CHANGE_ME`. Nunca publicar este archivo.

3. Construir e iniciar PostgreSQL, la aplicación y pgAdmin:

   ```bash
   docker compose up --build
   ```

4. Comprobar los healthchecks:

   ```bash
   docker compose ps
   ```

   Los servicios `db` y `app` deben aparecer como `healthy`.

5. Abrir:

   - Swagger UI: <http://localhost:8080/swagger-ui.html>
   - OpenAPI JSON: <http://localhost:8080/v3/api-docs>
   - Healthcheck: <http://localhost:8080/actuator/health>
   - pgAdmin: <http://localhost:8082>

Para detener los servicios:

```bash
docker compose down
```

Para borrar también los volúmenes y repetir desde cero:

```bash
docker compose down -v
```

Solo PostgreSQL, para desarrollar con Maven:

```bash
docker compose up -d db
```

## Flujo principal de la API

En el perfil `dev` la aplicación siembra datos de demostración al arrancar (`CargadorDatosSemilla`, idempotente): cuatro clientes con préstamos en distintos estados, de modo que Swagger y el reporte de cartera tienen datos desde el primer arranque.

| Método y ruta | Descripción |
|---|---|
| `POST /api/clientes` | Registra un cliente `INDIVIDUAL` o `EMPRESARIAL` (201) |
| `GET /api/clientes` · `GET /api/clientes/{id}` | Consulta de clientes |
| `POST /api/solicitudes` | Crea la solicitud en `Borrador` y responde **202**; la evaluación corre en segundo plano |
| `POST /api/solicitudes/{id}/evaluar` | Evaluación manual de una solicitud aún en `Borrador` |
| `GET /api/prestamos` · `GET /api/prestamos/{id}` | Consulta de préstamos y de su estado (`Aprobado`/`Rechazado` tras la evaluación) |
| `GET /api/prestamos/{id}/plan-pagos?metodo=FRANCES\|ALEMAN` | Plan de amortización; sin `metodo` usa el del producto |
| `POST /api/prestamos/{id}/desembolsar` | Desembolsa un préstamo aprobado y publica el evento de desembolso |
| `GET /api/reportes/cartera` | Resumen de cartera: totales, índice de mora, agrupaciones y conclusiones |

La evaluación asíncrona usa eventos nativos de Spring (`ApplicationEventPublisher` + `@Async @EventListener`): al crear una solicitud se publica `EventoSolicitudCreada`, `ListenerEvaluacion` ejecuta el motor de scoring en background y el estado transiciona `Borrador → EnEvaluacion → Aprobado/Rechazado`. El resultado se consulta con `GET /api/prestamos/{id}`.

## Persistencia y Flyway

Flyway ejecuta `src/main/resources/db/migration/V1__esquema_inicial.sql`. Hibernate usa `ddl-auto: validate`; no crea ni altera tablas.

- Herencia JPA `JOINED` para clientes y préstamos.
- Estado persistido mediante `estado_tipo` y columnas de datos.
- Entidades separadas del dominio en `persistencia.entidad`.
- Mapeadores manuales en `persistencia.mapper`.
- Más detalles: [docs/fase2-persistencia.md](docs/fase2-persistencia.md).

## Ejecutar los tests

```bash
mvn -B -ntp clean verify
```

El reporte de cobertura queda en `target/site/jacoco/index.html`.

Ejecutar una clase específica:

```bash
mvn test -Dtest=CalculadoraInteresTest
```

## Ejecución local sin Docker para la aplicación

La aplicación no contiene credenciales predeterminadas. Configure el entorno antes de iniciarla:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/prestamos"
$env:DB_USER="usuario_local"
$env:DB_PASSWORD="clave_local"
mvn spring-boot:run
```

## Seguridad opcional con API key

Para exigir una API key en `/api/**`, configure en `.env`:

```dotenv
APP_SECURITY_ENABLED=true
APP_API_KEY=una-clave-larga-aleatoria
```

Envíe la clave mediante el encabezado `X-API-Key`:

```bash
curl -H "X-API-Key: una-clave-larga-aleatoria" http://localhost:8080/api/prestamos
```

Swagger, OpenAPI y `/actuator/health` permanecen públicos. No coloque claves en código, capturas ni commits.

## CI/CD

`.github/workflows/ci.yml` contiene tres jobs:

1. `build-test`: configura Temurin 25, ejecuta `mvn clean verify` y publica JaCoCo y los resultados de tests.
2. `compose-smoke`: levanta `db` + `app` con Docker Compose desde cero, espera el healthcheck y verifica que la API responda con los datos semilla (`GET /api/reportes/cartera`).
3. `docker-image`: depende de los anteriores, construye la imagen y solo la publica cuando existen estos secrets:

   - `DOCKERHUB_USERNAME`
   - `DOCKERHUB_TOKEN`
   - `DOCKERHUB_IMAGE` (sin etiqueta, por ejemplo `usuario/prestamos-app`)

Sin esos secrets la imagen se construye para validación, pero no se publica.

## Arquitectura y patrones

La justificación de SOLID, Strategy, Factory y Observer está en [docs/arquitectura.md](docs/arquitectura.md).

## Diagnóstico rápido

- `docker` no se reconoce: instalar/iniciar Docker Desktop y abrir una terminal nueva.
- Maven usa otra versión: comprobar `mvn -version` y configurar `JAVA_HOME` hacia JDK 25.
- Falta una variable: crear `.env` desde `.env.example` y reemplazar todos los `CHANGE_ME`.
- La app espera a PostgreSQL: revisar `docker compose logs db`.
- Flyway falla: ejecutar `docker compose down -v` únicamente si se pueden borrar los datos locales y volver a levantar.
