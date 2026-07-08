# CLAUDE.md — Plataforma de Gestión y Evaluación de Préstamos

> Documento de contexto y reglas del proyecto. Su propósito es **delimitar el alcance**, fijar las decisiones de arquitectura y evitar que el sistema (en especial el motor de scoring) crezca sin control. Si una idea no está contemplada aquí, por defecto **queda fuera de alcance** hasta que el equipo la apruebe explícitamente.

---

## 1. Contexto del proyecto

Sistema de gestión y evaluación de solicitudes de préstamos para una institución financiera. Permite registrar clientes, capturar solicitudes, evaluarlas mediante un motor de scoring basado en reglas, generar planes de amortización y procesar de forma asíncrona las operaciones de mayor carga.

- **Curso:** Programación Avanzada en Java
- **Naturaleza:** Proyecto académico. La meta es demostrar dominio de Java moderno, SOLID, patrones de diseño y prácticas DevOps — **no** construir un producto bancario real.
- **Regla de oro:** ante la duda entre "más completo" y "más simple y terminado", elegir **simple y terminado**. Un alcance acotado y 100% funcional vale más que uno ambicioso a medias.

### Roles del equipo
| Integrante | Rol |
|---|---|
| Andrea Garrido | Persistencia (JPA, modelo de datos, migraciones) |
| Luis Humberto Ruiz | Dev/Ops y seguridad (Docker, CI/CD, configuración) |
| Luis Renato Granados | Backend (servicios, scoring, API REST) |

---

## 2. Stack tecnológico (FIJO — no cambiar sin acuerdo de equipo)

- **Lenguaje:** Java 25 (usar features modernas: records, sealed interfaces, pattern matching, switch expressions) y maven 3.9.6
- **Framework:** Spring Boot 3.x
- **Persistencia:** Spring Data JPA
- **Base de datos:** PostgreSQL (producción/desarrollo) · H2 en memoria (para tests)
- **Asincronía:** eventos nativos de Spring — `ApplicationEventPublisher`, `@EventListener`, `@Async`. **NO** Kafka, **NO** RabbitMQ, **NO** colas externas.
- **Documentación API:** springdoc-openapi (Swagger UI)
- **Validación:** Jakarta Bean Validation (`@Valid`, `@NotNull`, `@Positive`, etc.)
- **Tests:** JUnit 5 + Mockito + Testcontainers (para integración con BD)
- **Contenedor:** Docker (Dockerfile multi-stage)
- **CI/CD:** GitHub Actions (build + tests + reporte de cobertura)

> Si una necesidad parece requerir una tecnología fuera de esta lista, **probablemente está fuera de alcance**.

---

## 3. Arquitectura

### Decisión: Monolito modular (NO microservicios)
La rúbrica admite "microservicios **o** módulos bien definidos". Elegimos **monolito modular** definitivamente. No discutir microservicios, service discovery, API gateway ni comunicación entre servicios.

### Reglas de capas (innegociables)
- **Controllers**: solo reciben request, validan formato, delegan al service y devuelven response. **Cero lógica de negocio.**
- **Services**: toda la lógica de negocio. Es la capa que se prueba al 70%.
- **Repositories**: solo acceso a datos vía Spring Data JPA.
- Las **entidades JPA NUNCA salen por la API**. Siempre se mapea a DTOs (records).
- Dependencias apuntan hacia adentro: controller → service → repository. Nunca al revés.

### 3.1 Regla de arquitectura NO NEGOCIABLE

El paquete dominio es Java puro. Prohibido importar org.springframework.*, jakarta.persistence.* o cualquier framework dentro de gt.edu.umg.prestamos.dominio y sus subpaquetes. Todas las dependencias apuntan HACIA el dominio, nunca desde él. Si una tarea parece requerir una anotación de framework en el dominio, la solución correcta es crear un adaptador en la capa correspondiente (entidad JPA separada, DTO, configuración), no anotar el dominio.

gt.edu.umg.prestamos
├── dominio            # Java puro. Modelo del Entregable 1 + lógica de negocio
│   ├── cliente        # Cliente (abstract), ClienteIndividual, ClienteEmpresarial
│   ├── prestamo       # Prestamo (abstract), PrestamoPersonal/Hipotecario/Automotriz, Cuota
│   ├── estado         # EstadoPrestamo (sealed) + records: Borrador, EnEvaluacion,
│   │                  #   Aprobado, Rechazado, Desembolsado, Pagado, EnMora
│   ├── scoring        # ReglaScoring (interface), reglas concretas, MotorScoring
│   ├── calculo        # CalculadoraInteres (interface), MetodoFrances, MetodoAleman
│   └── analisis       # Pipeline Stream API de cartera (migrado de la app de consola)
├── persistencia       # Entidades JPA, repositorios Spring Data, mapeadores dominio<->entidad
├── servicio           # Servicios de aplicación, orquestación, eventos
├── api                # Controladores REST, DTOs (records), manejo de errores, OpenAPI
└── config             # Configuración Spring (async, beans, seguridad)

### 3.2 Convenciones

* Aplicar SOLID; en particular, las reglas de scoring son el ejemplo vivo de Open/Closed (se agregan reglas implementando la interfaz, sin tocar el motor — aunque para este proyecto el set ya está cerrado).
* Ramas: main (protegida) ← PR desde feature/<fase>-<descripcion> (ej. feature/f1-jerarquia-prestamo)
* Commits: convencionales — feat:, fix:, test:, docs:, chore:
* Dinero SIEMPRE con BigDecimal (nunca double); escala 2, RoundingMode.HALF_UP
* Fechas con java.time (LocalDate, LocalDateTime)
* Identificadores con UUID
* Todo método público de dominio con lógica no trivial debe tener test unitario
* Los tests corren con mvn verify y deben pasar antes de cada PR
* Idioma del código: nombres de clases/métodos en español según el Entregable 1 (ej. calcularCuota), comentarios en español
* Trabajar una fase a la vez, en el orden del roadmap. No adelantar código de fases futuras.
* Al terminar cada tarea: compilar (mvn compile), correr los tests (mvn verify) y reportar el resultado.
* Si una decisión de diseño no está especificada aquí, proponer la opción más simple que respete la regla de arquitectura y explicarla antes de implementar.
* No introducir dependencias nuevas al pom.xml sin justificarlo.
* Al crear tests, cubrir también casos límite: montos en cero o negativos, plazos inválidos, transiciones de estado no permitidas.

---

## 4. Modelo de dominio (cerrado)

### Jerarquías definidas — esta es la lista COMPLETA
- **Cliente** (abstract): `ClienteIndividual`, `ClienteEmpresarial`. No agregar más tipos.
- **Prestamo** (abstract): `PrestamoPersonal`, `PrestamoHipotecario`, `PrestamoAutomotriz`. No agregar más tipos.
- **EstadoPrestamo** (sealed interface, records): `Borrador`, `EnEvaluacion`, `Aprobado`, `Rechazado`, `Desembolsado`, `Pagado`, `EnMora`. Conjunto cerrado.
- **ReglaScoring** (interface): `ReglaEdad`, `ReglaIngreso`, `ReglaHistorial`, `ReglaCapacidadPago`. **Estas 4 son finales. No crear más reglas.**
- **CalculadoraInteres** (interface): `MetodoFrances`, `MetodoAleman`. Solo estos dos métodos.

### Tipos numéricos
- Todo monto, tasa, cuota o cálculo financiero usa **`BigDecimal`**, nunca `double`/`float`.
- Definir escala y `RoundingMode.HALF_UP` de forma consistente (sugerido: 2 decimales para montos).

---

## 5. Motor de scoring — DELIMITACIÓN ESTRICTA

> Esta es la sección más importante para evitar scope creep. **Leer antes de tocar cualquier cosa de scoring.**

### Principios
1. El scoring es **100% determinista y basado en reglas**. Mismo input → mismo output, siempre.
2. **PROHIBIDO**: integración con buró de crédito real, APIs externas, machine learning, "condiciones de mercado", datos en tiempo real. (La introducción del proyecto menciona el mercado solo como contexto narrativo; **no se implementa**.)
3. El historial crediticio es un **campo almacenado** en el cliente (enum `BUENO` / `REGULAR` / `MALO`), cargado manualmente o por seed. No se consulta a ningún sistema externo.

### Cómo funciona `MotorScoring`
- Recibe `Cliente` + `Prestamo` (la solicitud).
- Ejecuta las 4 reglas. Cada `ReglaScoring.evaluar(cliente, prestamo)` devuelve un **entero de 0 a 100**.
- Combina con **pesos fijos y constantes** (suman 100):

| Regla | Peso |
|---|---|
| `ReglaCapacidadPago` | 40 |
| `ReglaIngreso` | 25 |
| `ReglaHistorial` | 25 |
| `ReglaEdad` | 10 |

- Score final = promedio ponderado (0 a 100).
- **Decisión (solo dos resultados):** `score >= 60` → `Aprobado` · `score < 60` → `Rechazado`.

### Brackets fijos de cada regla (ejemplo de referencia — ajustar valores pero mantener la forma de tramos fijos)
- **ReglaCapacidadPago** (ratio = cuota estimada / capacidad de pago mensual): `<= 0.30` → 100 · `<= 0.40` → 70 · `<= 0.50` → 40 · resto → 0
- **ReglaIngreso**: por encima de umbral mínimo → 100 · escalonado debajo → 60/30/0
- **ReglaHistorial**: `BUENO` → 100 · `REGULAR` → 60 · `MALO` → 0
- **ReglaEdad**: dentro de rango válido (ej. 18–70 al final del plazo) → 100 · fuera → 0

> Los pesos y umbrales son **constantes documentadas en código**. Nunca se calculan dinámicamente ni se configuran por request.

---

## 6. Máquina de estados — flujo acotado

Implementar **solo el flujo feliz lineal** y un par de bifurcaciones necesarias:

```
Borrador → EnEvaluacion → Aprobado → Desembolsado → Pagado
                        ↘ Rechazado
        (Desembolsado puede pasar a EnMora si aplica la lógica de cuotas)
```

- No implementar todas las transiciones teóricas posibles entre los 7 estados.
- No construir un motor de workflow genérico ni un framework de transiciones configurable.
- Validar transiciones inválidas con una excepción de dominio simple.

---

## 8. Componente asíncrono

- Al crear una solicitud, se publica `EventoSolicitudCreada`.
- `ListenerEvaluacion` (`@Async @EventListener`) ejecuta el `MotorScoring` y actualiza el estado a `Aprobado`/`Rechazado`, publicando `EventoEvaluacionCompletada`.
- Al desembolsar, se publica `EventoPrestamoDesembolsado`, que `ListenerDesembolso` procesa.
- Configurar un `TaskExecutor` simple para `@Async`. No sobre-ingenierizar el pool.

---

## 9. Testing — meta: 70% en capa de servicio

- El **70% de cobertura aplica a la capa de service** (es lo que pide la rúbrica). Priorizar ahí.
- **Unitarios** (JUnit 5 + Mockito): cada regla de scoring, el motor, las calculadoras de interés, las transiciones de estado.
- **Integración** (Testcontainers + PostgreSQL): repositorios y al menos un flujo end-to-end de solicitud → evaluación.
- El scoring, al ser determinista, es **fácil de testear**: úsalo como núcleo de la cobertura.
- Generar reporte de cobertura (JaCoCo) y publicarlo en el pipeline.

---

## 10. DevOps

- **Dockerfile** multi-stage (build con Maven/Gradle → imagen runtime ligera con JRE).
- **docker-compose** opcional para levantar app + PostgreSQL en desarrollo.
- **CI/CD (GitHub Actions):** en cada push → compilar, correr tests, generar reporte de cobertura. Que falle el build si los tests fallan.
- **README** con: descripción, requisitos, cómo levantar con Docker, cómo correr tests, link a Swagger UI.
- Sin secretos en el repo. Configuración por variables de entorno / `application.yml` con perfiles.

---

## 11. FUERA DE ALCANCE (no implementar)

Lista explícita de tentaciones a rechazar:

- Microservicios, API gateway, service mesh.
- Kafka, RabbitMQ, o cualquier broker de mensajería externo.
- Integración con buró de crédito real o cualquier API externa.
- Machine learning o scoring "inteligente"/adaptativo.
- Autenticación/autorización compleja (OAuth2, JWT con roles granulares). Si se exige seguridad mínima, mantenerla básica.
- Multi-moneda, conversión de divisas, condiciones de mercado en tiempo real.
- Notificaciones por email/SMS, pasarelas de pago.
- Frontend complejo. Si hay UI, es secundaria y consume la API; **toda la nota está en el backend**.
- Motor de workflow genérico o reglas configurables por el usuario.
- Más tipos de cliente, préstamo, reglas de scoring o métodos de interés de los ya definidos.

---

# ROADMAP POR FASES
 
## FASE 0 — Fundación (prerequisito, si no existe aún)
 
1. Generar proyecto Maven con Spring Boot 3.x: dependencias `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `springdoc-openapi-starter-webmvc-ui`, `postgresql`, `h2` (scope test), `spring-boot-starter-test`.
2. Crear la estructura de paquetes de arriba (con `package-info.java` o `.gitkeep` donde esté vacío).
3. `docker-compose.yml` con PostgreSQL 16 (usuario/clave/db: `prestamos`), puerto 5432.
4. `application.yml` con perfil `dev` (Postgres local) y perfil `test` (H2 en memoria).
5. Workflow de GitHub Actions `.github/workflows/ci.yml`: en cada push/PR ejecutar `mvn -B verify` con JDK 25 (temurin).
6. `.gitignore` estándar de Maven/IDE.
**Criterio de aceptación:** `mvn verify` pasa en verde local y en CI; la app levanta con `mvn spring-boot:run` contra el Postgres de Docker.
 
## FASE 1 — Dominio puro (PRIORIDAD ACTUAL)
 
> Objetivo: implementar completa la jerarquía del Entregable 1 como Java puro, con tests. Esto desbloquea a persistencia (Fase 2) y servicios (Fase 3).
 
### 1.1 Jerarquía Cliente
- `Cliente` (abstract): campos `UUID id`, `String nombre`, `String documento`, `String email`, `LocalDate fechaRegistro`. Método concreto `calcularScoreBase(): int` (reutilizable) y abstracto `getCapacidadPago(): BigDecimal`.
- `ClienteIndividual`: `BigDecimal salarioMensual`, enum `TipoEmpleo` (FORMAL, INFORMAL, INDEPENDIENTE), `int antiguedadLaboral`. Capacidad de pago: 35% del salario mensual, con ajuste por tipo de empleo.
- `ClienteEmpresarial`: `BigDecimal facturacionAnual`, `String nit`, enum `SectorIndustria` (COMERCIO, INDUSTRIA, SERVICIOS, AGRICOLA, CONSTRUCCION). Capacidad de pago: 10% de la facturación mensualizada (facturacionAnual/12), con ajuste por sector.
- Validaciones en constructores: nombre/documento no vacíos, email con formato básico, valores monetarios positivos.
### 1.2 Jerarquía Préstamo
- `Prestamo` (abstract): `UUID id`, `Cliente cliente`, `BigDecimal monto`, `int plazoMeses`, `BigDecimal tasaAnual`, `EstadoPrestamo estado`, `LocalDate fechaSolicitud`. Abstractos: `calcularCuota()`, `calcularInteres()`. Concreto: `generarPlanPagos(): List<Cuota>` (usa la calculadora de interés, ver 1.4).
- `PrestamoPersonal`, `PrestamoHipotecario` (agrega `descripcionGarantia`, `BigDecimal avaluo`; validar monto <= 80% del avalúo), `PrestamoAutomotriz` (agrega `vehiculo`, `BigDecimal depreciacionAnual`).
- `Cuota` (record): `int numero`, `LocalDate fechaPago`, `BigDecimal capital`, `BigDecimal interes`, `BigDecimal total`.
- Validaciones: monto > 0, plazo entre 6 y 360 meses, tasa > 0.
### 1.3 Estados (sealed interface + records)
- `EstadoPrestamo` sealed interface que permite exactamente: `Borrador(LocalDateTime fechaCreacion)`, `EnEvaluacion(LocalDateTime fechaInicio, String evaluador)`, `Aprobado(LocalDateTime fechaAprobacion, int scoreObtenido)`, `Rechazado(LocalDateTime fechaRechazo, String motivo)`, `Desembolsado(LocalDateTime fecha, BigDecimal montoDesembolsado)`, `Pagado(LocalDateTime fechaUltimoPago)`, `EnMora(int diasAtraso, BigDecimal montoVencido)`.
- Método `Prestamo.descripcionEstado()` implementado con switch expression exhaustivo (pattern matching), como en el Entregable 1.
- Método de transición `Prestamo.cambiarEstado(EstadoPrestamo nuevo)` que valide transiciones legales (Borrador→EnEvaluacion→Aprobado/Rechazado; Aprobado→Desembolsado; Desembolsado→Pagado/EnMora; EnMora→Pagado) y lance `TransicionInvalidaException` en caso contrario.
### 1.4 Cálculo de intereses (Strategy)
- Interface `CalculadoraInteres`: `calcular(BigDecimal monto, int plazoMeses, BigDecimal tasaAnual): List<Cuota>`.
- `MetodoFrances`: cuota fija — `C = M * i / (1 - (1+i)^-n)` con `i = tasaAnual/12`.
- `MetodoAleman`: amortización constante de capital, interés sobre saldo decreciente.
- Toda aritmética con `BigDecimal` (usar `MathContext.DECIMAL64` en potencias/divisiones intermedias, redondear cuotas a 2 decimales).
### 1.5 Motor de scoring (Strategy)
- Definidio en la sección 5.
### 1.6 Análisis de cartera (migración de la app de consola)
- Migrar la lógica del proyecto de consola existente al paquete `dominio.analisis`: filtrado de préstamos con saldo, agrupación por riesgo (`groupingBy`) y por tipo, `summarizingDouble`/equivalente con BigDecimal para totales, promedio, mín, máx e índice de mora, y record `Resumen`.
- Mantenerla como lógica pura que recibe `List<Prestamo>` y devuelve estructuras de resultado (sin imprimir a consola; el formato del reporte será responsabilidad de la capa API en la Fase 4).
### 1.7 Tests de la Fase 1 (obligatorios)
- Cuota francesa vs. alemana contra valores calculados a mano (documentar en el test el cálculo esperado).
- Transiciones de estado válidas e inválidas (todas).
- Exhaustividad del switch de estados.
- Capacidad de pago por tipo de cliente.
- MotorScoring con reglas simuladas (aprueba/revisión/rechaza).
- Validaciones de constructores (datos incoherentes rechazados).
**Criterio de aceptación:** `mvn verify` en verde; cero imports de Spring/JPA en `dominio`; cobertura razonable de la lógica de cálculo y estados. Con esto Andrea puede iniciar la Fase 2 y Luis Renato la Fase 3 en paralelo.
 
## FASE 2 — Persistencia (Andrea)
 
1. Decidir e implementar la estrategia de mapeo: **entidades JPA separadas del dominio** (`ClienteEntity`, `PrestamoEntity` con herencia `JOINED` o `SINGLE_TABLE` justificada, `CuotaEntity`) + mapeadores manuales `dominio <-> entidad` en `persistencia.mapper`.
2. El estado del préstamo se persiste como columna discriminadora (`estado_tipo`) + columnas de datos del estado (o JSON), con mapper que reconstruye el record correcto del sealed interface.
3. Repositorios Spring Data: `ClienteRepository`, `PrestamoRepository` (consultas: por cliente, por estado, por tipo).
4. Restricciones de integridad: FKs, `nullable = false`, `unique` en documento/NIT, checks de montos positivos.
5. Migraciones con Flyway (`V1__esquema_inicial.sql`).
6. Tests de integración de repositorios con H2 o Testcontainers: guardar y recuperar cada tipo de cliente y préstamo, verificar que el estado se reconstruye correctamente.
**Criterio de aceptación:** ciclo completo guardar→leer→verificar para los 2 tipos de cliente, 3 tipos de préstamo y los 7 estados.
 
## FASE 3 — Servicios y API REST (Luis Renato, en paralelo con Fase 2)
 
1. Servicios en `servicio`: `ClienteService` (registrar, consultar), `SolicitudService` (crear solicitud en Borrador, enviar a evaluación), `EvaluacionService` (usa `MotorScoring` del dominio; las reglas se registran como beans y se inyectan como `List<ReglaScoring>` — documentar que esto es Strategy + OCP), `AmortizacionService` (genera plan de pagos; la calculadora se selecciona con un `CalculadoraInteresFactory` — documentar patrón Factory).
2. Mientras la Fase 2 no esté lista, programar contra las interfaces de repositorio con implementación en memoria (`repositorio.enmemoria`) para no bloquearse; sustituir por JPA al integrar.
3. Controladores REST en `api`: `POST/GET /api/clientes`, `POST /api/solicitudes`, `POST /api/solicitudes/{id}/evaluar`, `GET /api/prestamos/{id}/plan-pagos`, `GET /api/prestamos`.
4. DTOs como records (los del Entregable 1: `SolicitudPrestamoDTO`, `RespuestaPrestamoDTO`, `ClienteDTO`, `CuotaDTO`, `PlanPagosDTO`) + validación con `jakarta.validation` en los DTOs (permitido: los DTOs viven en `api`, no en `dominio`).
5. Manejo global de errores con `@RestControllerAdvice` (404, 400 con detalle de validación, 409 para transiciones inválidas).
6. OpenAPI: anotar controladores, verificar Swagger UI en `/swagger-ui.html`.
7. Tests: unitarios de servicios con Mockito + `@WebMvcTest` de controladores.
**Criterio de aceptación:** flujo completo por HTTP: crear cliente → crear solicitud → evaluar → obtener plan de pagos, demostrable en Swagger.
 
## FASE 4 — Asíncrono, eventos y reportería
 
1. Habilitar `@EnableAsync` en `config`. Records de eventos: `EventoSolicitudCreada`, `EventoEvaluacionCompletada`, `EventoPrestamoDesembolsado` (paquete `servicio.evento`).
2. Publicación con `ApplicationEventPublisher`; listeners `@Async @EventListener`: `ListenerEvaluacion` (evalúa la solicitud al crearse, en background) y `ListenerDesembolso` (simula notificación) — documentar que esto es el patrón Observer.
3. La evaluación asíncrona: al `POST /api/solicitudes` responder 202 con el id; el estado transiciona Borrador→EnEvaluacion→Aprobado/Rechazado vía listener.
4. Exponer el análisis de cartera: `GET /api/reportes/cartera` devuelve el resumen (totales, índice de mora, agrupaciones por riesgo y producto, conclusiones) generado por `dominio.analisis` sobre los préstamos persistidos.
5. Enriquecer `ReglaHistorial` para consultar préstamos previos del cliente vía repositorio (inyectado en la capa servicio, no en el dominio: la regla del dominio recibe los datos ya consultados).
6. Tests: listener procesa el evento (usar `Awaitility` o ejecutor síncrono en perfil test) y endpoint de reporte con datos semilla.
**Criterio de aceptación:** una solicitud creada queda evaluada automáticamente sin bloquear la respuesta HTTP; el reporte de cartera refleja los datos reales de la BD.
 
## FASE 5 — DevOps (Luis Humberto)
 
1. `Dockerfile` multi-stage (build con Maven, runtime con JRE 25 slim, usuario no root).
2. Extender `docker-compose.yml`: servicio `app` + `db`, healthchecks, variables de entorno para credenciales (nunca hardcodeadas).
3. Completar CI/CD: job de build+test, job de construcción de imagen Docker (y push a registry si hay credenciales configuradas como secrets).
4. Seguridad básica: validación estricta de entrada ya cubierta; agregar manejo de secretos por variables de entorno y, si el tiempo alcanza, Spring Security con API key simple.
5. Documentación técnica final (`docs/arquitectura.md`): justificar cada principio SOLID y cada patrón (Strategy en scoring y calculadoras, Factory en calculadoras, Observer en eventos) con referencia a las clases concretas — requisito del objetivo b del anteproyecto.
6. README del repositorio: cómo levantar todo con `docker compose up`, cómo correr tests, enlace a Swagger.
**Criterio de aceptación:** `docker compose up` levanta la aplicación completa desde cero; el pipeline CI/CD pasa en verde; la documentación técnica cubre SOLID y los 3 patrones.