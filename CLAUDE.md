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
| Luis Humberto Ruiz | Backend (servicios, scoring, API REST) |
| Luis Renato Granados | Dev/Ops y seguridad (Docker, CI/CD, configuración) |

---

## 2. Stack tecnológico (FIJO — no cambiar sin acuerdo de equipo)

- **Lenguaje:** Java 25 (usar features modernas: records, sealed interfaces, pattern matching, switch expressions)
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

### Organización por capas + módulos de dominio
```
com.compiladores.prestamos
├── cliente/        (controller, service, repository, model)
├── prestamo/       (controller, service, repository, model)
├── scoring/        (motor + reglas)
├── amortizacion/   (calculadoras de interés)
├── evento/         (publisher + listeners async)
├── shared/         (DTOs comunes, manejo de errores, config)
```

### Reglas de capas (innegociables)
- **Controllers**: solo reciben request, validan formato, delegan al service y devuelven response. **Cero lógica de negocio.**
- **Services**: toda la lógica de negocio. Es la capa que se prueba al 70%.
- **Repositories**: solo acceso a datos vía Spring Data JPA.
- Las **entidades JPA NUNCA salen por la API**. Siempre se mapea a DTOs (records).
- Dependencias apuntan hacia adentro: controller → service → repository. Nunca al revés.

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

## 7. API REST — endpoints (mínimo 5, ESTE es el conjunto)

No agregar endpoints "por si acaso". Estos cumplen la rúbrica:

1. `POST /api/clientes` — registrar cliente
2. `GET /api/clientes/{id}` — consultar cliente
3. `POST /api/prestamos/solicitudes` — crear solicitud (estado inicial `Borrador`/`EnEvaluacion`, dispara evaluación async)
4. `GET /api/prestamos/{id}` — consultar préstamo y su estado actual
5. `GET /api/prestamos/{id}/plan-pagos` — obtener plan de amortización (`PlanPagosDTO`)
6. *(opcional)* `POST /api/prestamos/{id}/desembolsar` — desembolsar un préstamo aprobado

### Requisitos transversales de la API
- **Validación completa** de cada DTO de entrada con Bean Validation.
- **Manejo de errores estructurado** vía `@RestControllerAdvice`: respuesta JSON uniforme (timestamp, status, código, mensaje, detalles). Definir excepciones de dominio (`ClienteNoEncontradoException`, `TransicionInvalidaException`, etc.).
- Todos los endpoints documentados en **OpenAPI/Swagger** con descripciones y ejemplos.
- DTOs de entrada/salida son **records**, nunca entidades JPA.

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

## 12. Convenciones de código

- Nombres en español para el dominio (coherente con el diseño), código de infraestructura puede ir en inglés.
- Un patrón = una justificación documentada (Strategy, Factory, Observer) en la documentación técnica.
- Aplicar SOLID; en particular, las reglas de scoring son el ejemplo vivo de Open/Closed (se agregan reglas implementando la interfaz, sin tocar el motor — aunque para este proyecto el set ya está cerrado).
- Commits descriptivos; ramas por feature; PRs revisados por al menos otro integrante.
- Sin lógica de negocio en controllers. Sin `double` para dinero. Sin entidades JPA en la API.
