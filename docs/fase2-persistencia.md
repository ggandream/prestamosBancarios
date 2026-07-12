# FASE 2 — Persistencia

> **Responsable:** Andrea Garrido · **Estado:** completada y verificada end-to-end
> (`mvn verify` en verde + `docker compose up` contra PostgreSQL real).

Documenta la capa de persistencia de la Plataforma de Gestión y Evaluación de Préstamos:
el mapeo objeto-relacional, las restricciones de integridad, las migraciones de esquema y
el entorno Docker para levantar la base de datos.

---

## 1. Alcance de la fase

De acuerdo con el plan del proyecto, esta fase cubrió:

1. Estrategia de mapeo: **entidades JPA separadas del dominio** + mapeadores manuales
   `dominio ↔ entidad`.
2. Persistencia del **estado del préstamo** (sealed interface `EstadoPrestamo`) con columna
   discriminadora + columnas de datos, reconstruyendo el record correcto al leer.
3. **Repositorios Spring Data** con consultas por cliente, por estado y por tipo.
4. **Restricciones de integridad**: PK/FK, `NOT NULL`, `UNIQUE` en documento/NIT, `CHECK`
   de montos positivos.
5. **Migraciones con Flyway** (`V1__esquema_inicial.sql`).
6. **Tests de integración** de repositorios: guardar y recuperar cada tipo de cliente y
   préstamo, y verificar que el estado se reconstruye correctamente.

Además, como parte de las tareas asignadas:

7. **`docker-compose.yml`** para levantar PostgreSQL y la aplicación (con instrucciones para
   el equipo), más un **`Dockerfile`** *starter* multi-stage y **pgAdmin** para inspección
   visual de la BD.

**Criterio de aceptación (cumplido):** ciclo completo guardar → leer → verificar para los
2 tipos de cliente, los 3 tipos de préstamo y los 7 estados.

---

## 2. Decisiones de diseño

| Decisión | Elección | Justificación |
|---|---|---|
| Herencia JPA | **`JOINED`** | Tabla base + tabla por subtipo con FK. Permite declarar `NOT NULL` real en los campos propios de cada subtipo (imposible con `SINGLE_TABLE`, donde deberían ser nullables), cumpliendo el requisito de restricciones de integridad. |
| Plan de cuotas | **Persistido** (`CuotaEntity`) | Relación `@OneToMany` préstamo→cuotas. El plan es determinista: se genera desde el dominio al guardar y se almacena como *snapshot* para consulta directa. |
| Estado del préstamo | **Aplanado** en columnas | Discriminador `estado_tipo` + columnas de datos genéricas nullables. Un mapper reconstruye el record exacto del sealed interface. |
| Entidades vs. dominio | **Separadas** | Las entidades JPA viven en `persistencia.entidad`; el dominio no tiene anotaciones de framework. Mapeadores manuales traducen entre ambos mundos. |
| Esquema | **Flyway** (fuente de verdad) | Hibernate solo **valida** (`ddl-auto: validate`); nunca crea ni altera tablas en `dev`. |

---

## 3. Estructura del paquete `persistencia`

```
gt.edu.umg.prestamos.persistencia
├── entidad/          Entidades JPA (@Entity, @Embeddable)
│   ├── ClienteEntity              (abstract, @Inheritance JOINED)
│   ├── ClienteIndividualEntity
│   ├── ClienteEmpresarialEntity
│   ├── PrestamoEntity             (abstract, @Inheritance JOINED)
│   ├── PrestamoPersonalEntity
│   ├── PrestamoHipotecarioEntity
│   ├── PrestamoAutomotrizEntity
│   ├── CuotaEntity                (@OneToMany desde PrestamoEntity)
│   ├── EstadoEmbeddable           (@Embeddable: estado aplanado)
│   └── TipoEstado                 (enum discriminador de estado)
├── mapper/           Mapeadores manuales dominio ↔ entidad
│   ├── ClienteMapper
│   ├── PrestamoMapper
│   ├── EstadoMapper
│   └── CuotaMapper
├── repositorio/      Repositorios Spring Data (sobre entidades)
│   ├── ClienteJpaRepository
│   └── PrestamoJpaRepository
└── adaptador/        Fachada que expone el dominio (oculta JPA)
    ├── ClienteRepositorioJpa
    └── PrestamoRepositorioJpa
```

**Flujo de dependencias:** `servicio (Fase 3) → adaptador → { repositorio, mapper } →
entidad`. Las entidades JPA **nunca** salen del paquete `persistencia`: los adaptadores
reciben y devuelven objetos de dominio.

---

## 4. Modelo de datos

### 4.1 Clientes (herencia `JOINED`)

```
cliente (id, tipo_cliente, nombre, documento⯑, email, fecha_registro, historial)
  ├── cliente_individual   (id⯈, salario_mensual, tipo_empleo, antiguedad_laboral)
  └── cliente_empresarial  (id⯈, facturacion_anual, nit⯑, sector, antiguedad_nit)
```

### 4.2 Préstamos (herencia `JOINED`)

```
prestamo (id, tipo_prestamo, cliente_id⯈, monto, plazo_meses, tasa_anual,
          fecha_solicitud, estado_tipo, estado_fecha, estado_texto,
          estado_score, estado_monto, estado_dias_atraso)
  ├── prestamo_personal      (id⯈)
  ├── prestamo_hipotecario   (id⯈, descripcion_garantia, avaluo)
  └── prestamo_automotriz    (id⯈, vehiculo, depreciacion_anual)

cuota (id, prestamo_id⯈, numero, fecha_pago, capital, interes, total)
```

> `⯑` = restricción `UNIQUE` · `⯈` = clave foránea (FK)

### 4.3 Tipos de datos

- Montos, cuotas y avalúos: `NUMERIC(18,2)`.
- Tasa anual: `NUMERIC(10,6)` (fracción decimal, ej. `0.120000`).
- Identificadores: `UUID` (generados en el dominio).
- Enumerados (`historial`, `tipo_empleo`, `sector`, `estado_tipo`): `VARCHAR` con `@Enumerated(STRING)`.
- Fechas: `DATE` (fecha de solicitud/registro) y `TIMESTAMP` (fechas de estado).

---

## 5. Persistencia del estado del préstamo

El dominio modela el estado como un **sealed interface** con 7 records
(`EstadoPrestamo`). Para persistirlo sin perder tipado ni información se usó la estrategia
**discriminador + columnas de datos aplanadas** (`EstadoEmbeddable`):

| Estado (record) | `estado_tipo` | Columnas de datos usadas |
|---|---|---|
| `Borrador(fechaCreacion)` | `BORRADOR` | `estado_fecha` |
| `EnEvaluacion(fechaInicio, evaluador)` | `EN_EVALUACION` | `estado_fecha`, `estado_texto` |
| `Aprobado(fechaAprobacion, scoreObtenido)` | `APROBADO` | `estado_fecha`, `estado_score` |
| `Rechazado(fechaRechazo, motivo)` | `RECHAZADO` | `estado_fecha`, `estado_texto` |
| `Desembolsado(fecha, montoDesembolsado)` | `DESEMBOLSADO` | `estado_fecha`, `estado_monto` |
| `Pagado(fechaUltimoPago)` | `PAGADO` | `estado_fecha` |
| `EnMora(diasAtraso, montoVencido)` | `EN_MORA` | `estado_dias_atraso`, `estado_monto` |

- **De dominio a entidad**: `EstadoMapper` usa un `switch` con *pattern matching*
  exhaustivo (sin `default`, garantizado por el sealed interface) para repartir cada campo
  en su columna.
- **De entidad a dominio**: un `switch` sobre `TipoEstado` reconstruye el record exacto.

### Rehidratación del agregado

El dominio (`Prestamo`) valida las transiciones de estado en `cambiarEstado(...)`. Para
reconstruir un préstamo leído de la BD **en cualquier estado** (sin re-ejecutar la máquina
de estados, que rechazaría, p. ej., pasar directo a `EnMora`) se añadió al dominio el método
`Prestamo.restaurarEstado(EstadoPrestamo)`, documentado como de **uso exclusivo de la capa
de persistencia**.

> ⚠️ **Nota de coordinación:** este método vive en el código de la Fase 1 (dominio, de
> Renato). Es el único cambio que la Fase 2 introdujo fuera del paquete `persistencia`.

---

## 6. Repositorios y consultas

Repositorios Spring Data sobre entidades:

- **`ClienteJpaRepository`** — `findByDocumento`, `existsByDocumento`.
- **`PrestamoJpaRepository`**:
  - `findByClienteId(UUID)` — préstamos de un cliente.
  - `findByEstadoTipo(TipoEstado)` — préstamos en un estado (deriva sobre el campo embebido).
  - `findByTipo(Class<? extends PrestamoEntity>)` — préstamos de un subtipo, usando
    `type(p) = :tipo` sobre la jerarquía `JOINED`.

Los **adaptadores** (`ClienteRepositorioJpa`, `PrestamoRepositorioJpa`) envuelven estos
repositorios y exponen operaciones en términos del dominio (`guardar`, `buscarPorId`,
`buscarPorCliente`, `buscarPorEstado`, `buscarPorTipo`). Al guardar un préstamo, el
adaptador resuelve la `ClienteEntity` gestionada por su id antes de mapear, garantizando la
integridad referencial de `cliente_id` sin cascadear la persistencia del cliente.

---

## 7. Restricciones de integridad (`V1__esquema_inicial.sql`)

La migración Flyway define:

- **Claves primarias** en todas las tablas; **claves foráneas** de cada subtipo a su tabla
  base, de `prestamo.cliente_id` a `cliente`, y de `cuota.prestamo_id` a `prestamo`.
- **`UNIQUE`**: `cliente.documento`, `cliente_empresarial.nit`, y
  `(cuota.prestamo_id, cuota.numero)`.
- **`NOT NULL`** en todos los campos obligatorios (incluidos los propios de cada subtipo,
  posible gracias a `JOINED`).
- **`CHECK`**:
  - montos positivos: `monto > 0`, `salario_mensual > 0`, `facturacion_anual > 0`,
    `avaluo > 0`, `depreciacion_anual > 0`.
  - `plazo_meses BETWEEN 6 AND 360`, `tasa_anual > 0`, `cuota.numero > 0`,
    antigüedades `>= 0`.
  - dominios de enumerados: `historial`, `tipo_empleo`, `sector`, `estado_tipo`.
- **Índices** en `prestamo.cliente_id`, `prestamo.estado_tipo` y `cuota.prestamo_id`.

---

## 8. Configuración por perfil

| Perfil | Base de datos | Esquema | Uso |
|---|---|---|---|
| `dev` | PostgreSQL | **Flyway** aplica `V1`; Hibernate `ddl-auto: validate` | Ejecución local / Docker |
| `test` | H2 (modo PostgreSQL) | Hibernate `create-drop` (Flyway desactivado) | Tests automatizados |

En `dev`, Flyway es la fuente de verdad del esquema y Hibernate solo valida que las
entidades coincidan. En `test`, Flyway se desactiva (su SQL usa DDL de PostgreSQL) y el
esquema lo genera Hibernate desde las entidades, lo que valida el mapeo JPA sin depender
del dialecto.

---

## 9. Tests

Ubicados en `src/test/java/.../persistencia`:

- **`PersistenciaIntegracionTest`** (14 tests) — ciclo guardar → leer → verificar a través
  de los adaptadores de dominio:
  - los **2 tipos de cliente** (individual y empresarial), incluida consulta por documento;
  - los **3 tipos de préstamo** (personal, hipotecario, automotriz), verificando además
    que el plan de cuotas se persiste y las consultas por tipo/estado funcionan;
  - los **7 estados** (test parametrizado), comprobando que cada record se reconstruye
    idéntico (igualdad por valor).
- **`MigracionEsquemaTest`** (1 test) — activa Flyway sobre H2 con `ddl-auto: validate`:
  si el contexto arranca, la migración `V1` y las entidades JPA están **sincronizadas**
  (mismas tablas, columnas, tipos, escalas). Protege contra deriva esquema↔entidades.

**Resultado:** `mvn verify` → **53 tests, 0 fallos**.

---

## 10. Entorno Docker

`docker-compose.yml` define tres servicios (todos para **desarrollo**):

| Servicio | Imagen | Puerto host | Descripción |
|---|---|---|---|
| `db` | `postgres:16-alpine` | `5432` | PostgreSQL con volumen persistente y healthcheck (`pg_isready`). |
| `app` | *build* (`Dockerfile`) | `8080` | Aplicación Spring Boot (perfil `dev`). Espera a que `db` esté *healthy*. |
| `pgadmin` | `dpage/pgadmin4` | `8082` | Administrador web de la BD, con `prestamos-db` pre-registrado. |

### Comandos

```bash
docker compose up --build      # levanta db + app + pgadmin desde cero
docker compose up -d db        # solo la base de datos (para desarrollar la app en local)
docker compose down            # detiene y elimina contenedores (conserva volúmenes)
docker compose down -v         # además borra los datos
```

### Accesos

- **API / Swagger UI:** `http://localhost:8080/swagger-ui.html`
  *(sin operaciones aún: los endpoints REST son de la Fase 3).*
- **pgAdmin:** `http://localhost:8082` — login `admin@admin.com` / `admin123`. El servidor
  `prestamos-db` ya aparece; al conectarse pide la contraseña de la BD (`prestamos`).

### Variables de entorno (con valores por defecto)

| Variable | Default | Uso |
|---|---|---|
| `DB_NAME` | `prestamos` | nombre de la base de datos |
| `DB_USER` | `prestamos` | usuario de la BD |
| `DB_PASSWORD` | `prestamos` | contraseña de la BD |
| `DB_PORT` | `5432` | puerto de PostgreSQL en el host |
| `APP_PORT` | `8080` | puerto de la app en el host |
| `PGADMIN_PORT` | `8082` | puerto de pgAdmin en el host |
| `PGADMIN_EMAIL` | `admin@admin.com` | usuario de pgAdmin (evitar dominios reservados como `.local`) |
| `PGADMIN_PASSWORD` | `admin123` | contraseña de pgAdmin (mínimo 6 caracteres) |

Las credenciales nunca están hardcodeadas: se resuelven por variables de entorno y pueden
sobreescribirse con un archivo `.env` en la raíz.

> **Coordinación con la Fase 5 (DevOps, Luis):** el `Dockerfile` del servicio `app` es un
> *starter* multi-stage funcional; Luis lo optimizará (cacheo de capas de dependencias,
> JRE slim, healthcheck de la app). Los servicios `db` y `pgadmin` ya son definitivos.

---

## 11. Archivos entregados

**Código y esquema**
- `src/main/java/.../persistencia/**` — entidades, mappers, repositorios y adaptadores.
- `src/main/resources/db/migration/V1__esquema_inicial.sql` — migración Flyway.
- `src/main/java/.../dominio/prestamo/Prestamo.java` — método `restaurarEstado(...)` (único
  cambio en el dominio; ver §5).

**Configuración**
- `src/main/resources/application.yml` — perfil `dev` (Flyway + validate).
- `src/test/resources/application.yml` — perfil `test` (H2, Flyway off).
- `pom.xml` — dependencias `flyway-core` y `flyway-database-postgresql`.

**Tests**
- `src/test/java/.../persistencia/PersistenciaIntegracionTest.java`
- `src/test/java/.../persistencia/MigracionEsquemaTest.java`

**Docker**
- `docker-compose.yml` — servicios `db`, `app`, `pgadmin`.
- `Dockerfile` — build multi-stage *starter*.
- `.dockerignore`
- `docker/pgadmin/servers.json` — pre-registro del servidor en pgAdmin.
