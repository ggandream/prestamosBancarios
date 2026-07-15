# Arquitectura, principios SOLID y patrones de diseño

## 1. Alcance y estilo arquitectónico

La solución es un **monolito modular**. El dominio se mantiene como Java puro en `gt.edu.umg.prestamos.dominio`; las capas externas de API, servicios, persistencia y configuración pueden depender del dominio, pero el dominio no importa Spring ni JPA. Esta dirección de dependencias facilita probar las reglas financieras sin infraestructura.

La rama base actual contiene las Fases 0–2: dominio, Strategy y persistencia JPA con Flyway. Las referencias de Factory y Observer definidas por el diseño se materializan al integrar las ramas de Fases 3–4. La lista de comprobación de la sección 5 evita afirmar que esas clases ya están presentes antes de la integración final.

## 2. Principios SOLID

### 2.1 Responsabilidad única (SRP)

Cada clase tiene un motivo principal de cambio:

- `MotorScoring` coordina reglas y calcula el resultado ponderado; no conoce HTTP, JPA ni Docker.
- `ReglaEdad`, `ReglaIngreso`, `ReglaHistorial` y `ReglaCapacidadPago` encapsulan cada criterio de evaluación.
- `MetodoFrances` y `MetodoAleman` contienen únicamente algoritmos de amortización.
- `AnalizadorCartera` agrega y resume préstamos sin presentar resultados por consola ni API.
- `ClienteMapper`, `PrestamoMapper`, `EstadoMapper` y `CuotaMapper` traducen entre dominio y persistencia sin asumir responsabilidades de repositorio.
- `ClienteJpaRepository` y `PrestamoJpaRepository` se limitan al acceso de datos; `ClienteRepositorioJpa` y `PrestamoRepositorioJpa` adaptan esos repositorios al dominio.
- `ApiKeyAuthenticationFilter` valida el encabezado `X-API-Key`; `SecurityConfig` decide qué rutas se protegen.

Esta separación reduce cambios cruzados: modificar el algoritmo alemán no obliga a tocar scoring o seguridad.

### 2.2 Abierto/cerrado (OCP)

`MotorScoring` trabaja contra `ReglaScoring`. Una nueva política compatible podría añadirse implementando la interfaz e incorporándola a la lista inyectada, sin modificar el motor. De forma equivalente, `Prestamo.generarPlanPagos(CalculadoraInteres)` acepta cualquier implementación del contrato `CalculadoraInteres`; los algoritmos existentes son `MetodoFrances` y `MetodoAleman`.

El conjunto de estrategias está deliberadamente cerrado por el alcance académico, pero la estructura demuestra OCP sin crear complejidad innecesaria.

### 2.3 Sustitución de Liskov (LSP)

- `ClienteIndividual` y `ClienteEmpresarial` pueden utilizarse donde se espera un `Cliente`; ambos respetan los contratos de ingreso y capacidad de pago.
- `PrestamoPersonal`, `PrestamoHipotecario` y `PrestamoAutomotriz` conservan las invariantes de `Prestamo` y proporcionan una calculadora por defecto válida.
- `MetodoFrances` y `MetodoAleman` devuelven una `List<Cuota>` válida para los mismos parámetros admitidos por `CalculadoraInteres`.

Ningún subtipo debilita las validaciones base ni obliga al consumidor a preguntar por su clase concreta para ejecutar el comportamiento común.

### 2.4 Segregación de interfaces (ISP)

`ReglaScoring` expone solo `evaluar`, `peso` y `descripcion`, lo necesario para el motor. `CalculadoraInteres` expone únicamente el cálculo del plan y el nombre del método. Los consumidores no dependen de operaciones de persistencia, presentación o seguridad que no utilizan.

### 2.5 Inversión de dependencias (DIP)

Los componentes de alto nivel dependen de abstracciones:

- `MotorScoring` depende de `List<ReglaScoring>`, no de las cuatro reglas concretas.
- `Prestamo` depende de `CalculadoraInteres`, no de un algoritmo fijo.
- `ClienteRepositorioJpa` y `PrestamoRepositorioJpa` dependen de contratos de repositorio y mapeadores; las entidades `ClienteEntity`, `PrestamoEntity` y `CuotaEntity` permanecen fuera del dominio.
- En la integración de Fase 3, `EvaluacionService` debe recibir las reglas y `AmortizacionService` debe usar `CalculadoraInteresFactory`, evitando construir dependencias dentro de los servicios.

El paquete de dominio no importa `org.springframework.*` ni `jakarta.persistence.*`, lo que confirma que las dependencias apuntan hacia adentro.

## 3. Patrones de diseño

### 3.1 Strategy en scoring

**Contexto:** `MotorScoring`.

**Estrategia:** `ReglaScoring`.

**Estrategias concretas:** `ReglaEdad`, `ReglaIngreso`, `ReglaHistorial` y `ReglaCapacidadPago`.

El motor recorre una colección de estrategias, ejecuta `evaluar(cliente, prestamo)` y combina los resultados mediante `peso()`. La selección por composición evita un bloque condicional con todas las reglas y permite probar cada criterio aisladamente.

### 3.2 Strategy en calculadoras de interés

**Contexto:** `Prestamo.generarPlanPagos(CalculadoraInteres)` y, tras integrar Fase 3, `AmortizacionService`.

**Estrategia:** `CalculadoraInteres`.

**Estrategias concretas:** `MetodoFrances` y `MetodoAleman`.

Ambos algoritmos comparten el mismo contrato y producen cuotas con `BigDecimal`. El llamador elige el método sin cambiar la lógica interna del préstamo.

### 3.3 Factory en calculadoras

**Factory prevista por Fase 3:** `CalculadoraInteresFactory`.

**Productos:** implementaciones de `CalculadoraInteres`: `MetodoFrances` y `MetodoAleman`.

**Cliente:** `AmortizacionService`.

La Factory centraliza la traducción de una opción de entrada (por ejemplo, `FRANCES` o `ALEMAN`) a la estrategia correspondiente. Así, el controlador y el servicio no usan `new MetodoFrances()` ni contienen condicionales de construcción. La Factory devuelve la abstracción `CalculadoraInteres`, reforzando DIP y OCP.

### 3.4 Observer mediante eventos de Spring

**Publicador previsto por Fases 3–4:** `SolicitudService` mediante `ApplicationEventPublisher`.

**Eventos:** `EventoSolicitudCreada`, `EventoEvaluacionCompletada` y `EventoPrestamoDesembolsado`.

**Observadores:** `ListenerEvaluacion` y `ListenerDesembolso`, registrados con `@EventListener` y ejecutados con `@Async`.

El emisor publica hechos de negocio sin invocar directamente a cada receptor. Los listeners reaccionan de forma desacoplada: `ListenerEvaluacion` inicia el scoring y `ListenerDesembolso` procesa las acciones posteriores al desembolso. Spring actúa como canal de notificación del patrón Observer sin introducir Kafka o RabbitMQ.

## 4. Decisiones DevOps y seguridad

- El `Dockerfile` usa una etapa Maven con Temurin 25 y una etapa final `eclipse-temurin:25-jre-alpine` ligera.
- La imagen final ejecuta el JAR con el usuario no root `prestamos`.
- `docker-compose.yml` espera a que PostgreSQL esté saludable antes de iniciar la aplicación y verifica `/actuator/health`.
- `DB_USER`, `DB_PASSWORD` y la API key se reciben mediante variables de entorno; `.env` está excluido de Git.
- `ApiKeyAuthenticationFilter` compara la clave sin almacenarla en código y solo protege `/api/**` cuando `APP_SECURITY_ENABLED=true`.
- CI separa `build-test` de `docker-image`; una imagen con tests fallidos nunca llega al job de Docker.
- El push al registry es opcional y depende de secrets de GitHub, por lo que el workflow también funciona en forks sin credenciales.

## 5. Verificación antes de fusionar todas las fases

En la rama integrada final se debe confirmar con `rg` o búsqueda del IDE que existan estas clases:

- `CalculadoraInteresFactory` y `AmortizacionService` (Fase 3).
- `SolicitudService` con `ApplicationEventPublisher` (Fases 3–4).
- `EventoSolicitudCreada`, `EventoEvaluacionCompletada`, `EventoPrestamoDesembolsado`, `ListenerEvaluacion` y `ListenerDesembolso` (Fase 4).

Si los nombres cambian durante la integración, se deben actualizar las referencias de las secciones 2.5, 3.3 y 3.4. Después se ejecutan:

```bash
mvn -B -ntp clean verify
docker compose up --build
docker compose ps
```

La aceptación final requiere tests verdes, servicios `db` y `app` saludables, Swagger accesible y esta documentación alineada con las clases realmente fusionadas.
