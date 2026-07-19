# GUIDES.md — Guía práctica del proyecto

Guía para entender cómo funciona la plataforma, qué se puede probar y cómo ponerla a
correr desde cero en una PC con WSL. Complementa al [README](README.md) (operación) y
a [docs/arquitectura.md](docs/arquitectura.md) (justificación de SOLID y patrones).

---

## 1. Cómo funciona el proyecto

Es un **monolito modular** en Spring Boot. El corazón es el paquete
`gt.edu.umg.prestamos.dominio`: Java puro, sin ninguna anotación de framework. Las
demás capas (API, servicios, persistencia, configuración) dependen del dominio, nunca
al revés.

```
HTTP → api (controllers, DTOs) → servicio (casos de uso, eventos) → persistencia (JPA) → PostgreSQL
                                        ↓ usa
                                  dominio (Java puro: clientes, préstamos, estados, scoring, cálculo)
```

### El ciclo de vida de un préstamo

1. **Se registra un cliente** (`INDIVIDUAL` o `EMPRESARIAL`). El dominio calcula su
   capacidad de pago: 35% del salario ajustado por tipo de empleo, o 10% de la
   facturación mensualizada ajustada por sector.
2. **Se crea una solicitud** (`PERSONAL`, `HIPOTECARIO` o `AUTOMOTRIZ`). Nace en
   estado `Borrador` y la API responde **202 Accepted**: la evaluación no bloquea la
   respuesta.
3. **La evaluación corre en segundo plano** (patrón Observer): al crear la solicitud
   se publica `EventoSolicitudCreada`; `ListenerEvaluacion` (`@Async @EventListener`)
   ejecuta el `MotorScoring` y el estado transiciona
   `Borrador → EnEvaluacion → Aprobado/Rechazado`.
4. **El scoring es 100% determinista** (mismos datos, mismo resultado). Promedio
   ponderado de 4 reglas; aprueba con score ≥ 60:

   | Regla | Peso | Qué mira |
   |---|---|---|
   | Capacidad de pago | 40 | ratio cuota estimada / capacidad mensual |
   | Ingreso | 25 | ingreso mensual contra umbrales fijos |
   | Historial | 25 | historial almacenado (BUENO/REGULAR/MALO) **+ préstamos previos**: mora previa → 0, préstamo pagado → bono |
   | Antigüedad | 10 | años de vínculo laboral / NIT |

5. **Desembolso**: un préstamo `Aprobado` puede desembolsarse
   (`Aprobado → Desembolsado`); se publica `EventoPrestamoDesembolsado` y
   `ListenerDesembolso` simula la notificación en el log.
6. **Plan de pagos**: cada producto tiene su método de amortización por defecto
   (francés para personal/hipotecario, alemán para automotriz) y puede pedirse el
   otro explícitamente (patrón Factory).
7. **Reporte de cartera**: agregaciones con Stream API sobre los préstamos
   persistidos — totales, índice de mora, distribución por riesgo y producto,
   conclusiones.

En el perfil `dev`, al arrancar se **siembran datos de demostración** (idempotente):
María López (préstamo desembolsado), Carlos Pérez (rechazado), Ferretería El Tornillo
(aprobado sin desembolsar) y Diego Martínez (en mora, para que el reporte muestre un
índice de mora real).

---

## 2. Endpoints de la API

Swagger UI interactivo en <http://localhost:8080/swagger-ui.html>.

| Método y ruta | Qué hace | Respuestas |
|---|---|---|
| `POST /api/clientes` | Registra un cliente | 201 · 400 datos inválidos/documento duplicado |
| `GET /api/clientes` | Lista clientes | 200 |
| `GET /api/clientes/{id}` | Cliente por id | 200 · 404 |
| `POST /api/solicitudes` | Crea solicitud en `Borrador`; evaluación en background | **202** · 400 · 404 cliente inexistente |
| `POST /api/solicitudes/{id}/evaluar` | Evaluación manual (solo si sigue en `Borrador`) | 200 · 404 · 409 ya evaluada |
| `GET /api/prestamos` | Lista préstamos con su estado | 200 |
| `GET /api/prestamos/{id}` | Préstamo por id (para consultar el resultado de la evaluación) | 200 · 404 |
| `GET /api/prestamos/{id}/plan-pagos?metodo=FRANCES\|ALEMAN` | Plan de amortización; sin `metodo` usa el del producto | 200 · 400 método inválido · 404 |
| `POST /api/prestamos/{id}/desembolsar` | Desembolsa un préstamo aprobado | 200 · 404 · 409 no está aprobado |
| `GET /api/reportes/cartera` | Resumen de cartera con conclusiones | 200 |

Códigos de error uniformes vía `ManejadorGlobalErrores`: 400 (validación), 404
(recurso inexistente), 409 (transición de estado ilegal).

---

## 3. Qué probar en el frontend

Con backend y frontend arriba, abrir <http://localhost:5174>. Tres pestañas:

**Préstamos** (la más ilustrativa)
- *Nueva solicitud* → elegir cliente, producto, monto, plazo y tasa. Al enviar, la UI
  muestra "aceptada (202)…" y la tabla se refresca sola: se ve el badge pasar de
  `Borrador` → `En evaluación` → `Aprobado`/`Rechazado` **en vivo** (esa es la
  evaluación asíncrona de la Fase 4).
- *Desembolsar* (solo aparece en préstamos aprobados) → cambia el estado y avisa que
  se publicó el evento de desembolso.
- *Plan de pagos* → modal con todas las cuotas; comparar método **francés** (cuota
  fija) contra **alemán** (cuota decreciente) con el selector.

**Clientes**
- Registrar clientes individuales o empresariales; la tabla muestra la capacidad de
  pago que calcula el dominio (útil para explicar por qué un préstamo se aprueba o no).

**Cartera**
- El reporte como dashboard: totales, índice de mora, exposición por producto,
  distribución por riesgo y conclusiones. Se actualiza al crear/desembolsar préstamos.

---

## 4. Demo recomendada (~5 minutos)

1. **Arranque**: `docker compose up -d db app` + frontend (`npm run dev`). Abrir la
   pestaña **Cartera**: ya hay 4 préstamos sembrados y un índice de mora de ~28.6% —
   explicar que salió del seed.
2. **El contraste aprobado/rechazado**: en **Clientes**, registrar un cliente
   individual sólido (salario Q9,000, empleo FORMAL, historial BUENO, 5 años). En
   **Préstamos**, crearle una solicitud personal de Q40,000 a 48 meses al 12% → ver
   el badge aprobar en ~2 segundos. Luego crear una solicitud para **Carlos Pérez**
   (el del seed: informal, historial MALO) → se rechaza. Mismo flujo, mismas reglas,
   resultado determinista.
3. **La regla enriquecida (Fase 4)**: crear una solicitud para **Diego Martínez** —
   tiene un préstamo en mora, así que aunque su perfil aprueba en las demás reglas,
   el historial se anula y la solicitud se rechaza. Buen momento para explicar que la
   regla consulta préstamos previos sin que el dominio toque la base de datos.
4. **Desembolso y eventos**: desembolsar el préstamo recién aprobado. En una
   terminal, mostrar el backend reaccionando:

   ```bash
   docker compose logs app | grep -E "evaluacion_|notificacion_"
   ```

   Ahí se ven los hilos `prestamos-async-*` (evaluaciones) y la notificación simulada
   del desembolso — el patrón Observer trabajando.
5. **Plan de pagos**: abrir el plan del préstamo automotriz del seed y alternar
   francés/alemán para mostrar la diferencia de cuotas (Strategy + Factory).
6. **Cierre en Cartera**: el reporte ya refleja los préstamos nuevos. Para el jurado
   técnico, rematar con Swagger (<http://localhost:8080/swagger-ui.html>) mostrando
   el 202 y el 409 al re-evaluar.

---

## 5. Cómo ejecutar el proyecto en WSL

### Versiones que usa el proyecto

| Herramienta | Versión | Para qué |
|---|---|---|
| Java (Temurin) | **25** | compilar/correr el backend fuera de Docker |
| Maven | **3.9.9** (mínimo 3.9.x) | build y tests |
| Node.js | **22 LTS** (mínimo 20.19) | frontend (Vite 8) |
| Docker + Compose v2 + **buildx** | reciente | PostgreSQL y la app en contenedores |

> Si solo se quiere **ver el proyecto funcionando**, basta Docker y Node (opción A).
> Java y Maven solo hacen falta para desarrollar/correr tests (opción B).

### 5.1 Preparar WSL

En PowerShell (una sola vez, si no tiene WSL):

```powershell
wsl --install -d Ubuntu
```

Instalar **Docker Desktop** en Windows y activar *Settings → Resources → WSL
integration* para la distro Ubuntu. Verificar dentro de WSL:

```bash
docker compose version   # debe ser v2
docker buildx version    # necesario: el Dockerfile usa cache mounts de BuildKit
```

> Si `docker buildx version` falla (Docker instalado directo en WSL, sin Desktop),
> instalar el plugin: descargar el binario de
> <https://github.com/docker/buildx/releases> a `~/.docker/cli-plugins/docker-buildx`
> y darle permisos de ejecución (`chmod +x`).

### 5.2 Opción A — Todo con Docker (lo más rápido)

```bash
git clone <url-del-repositorio> prestamosBancarios
cd prestamosBancarios

cp .env.example .env
nano .env        # reemplazar TODOS los valores CHANGE_ME (cualquier valor local sirve)

docker compose up --build -d db app
docker compose ps    # esperar a que db y app estén "healthy" (~1-2 min la primera vez)
```

Listo: Swagger en <http://localhost:8080/swagger-ui.html> con los datos semilla ya
cargados. Para bajar todo: `docker compose down` (con `-v` borra también los datos).

### 5.3 Frontend

Instalar Node con nvm (recomendado en WSL):

```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
exec bash
nvm install 22
```

Y correr la SPA:

```bash
cd frontend
npm install
npm run dev      # http://localhost:5174 (proxya /api al backend en :8080)
```

### 5.4 Opción B — Desarrollo backend (Java + Maven locales)

Instalar SDKMAN y las versiones exactas:

```bash
curl -s "https://get.sdkman.io" | bash
exec bash
sdk install java 25-tem      # Temurin 25
sdk install maven 3.9.9
java -version && mvn -version   # confirmar 25 y 3.9.9
```

Levantar solo PostgreSQL y correr la app con Maven:

```bash
docker compose up -d db

export DB_URL="jdbc:postgresql://localhost:5432/prestamos"
export DB_USER="<el DB_USER del .env>"
export DB_PASSWORD="<el DB_PASSWORD del .env>"
mvn spring-boot:run
```

Correr la suite de tests (no necesita base de datos, usa H2 en memoria):

```bash
mvn verify
# cobertura: target/site/jacoco/index.html
```

### 5.5 Problemas comunes

- **`mvn` falla con "requires Maven 3.6.3"** → hay un Maven viejo en el PATH;
  `sdk default maven 3.9.9` y abrir una terminal nueva.
- **`docker build` falla con "the --mount option requires BuildKit"** → falta el
  plugin buildx (ver 5.1).
- **Puerto 5432/8080 ocupado** → cambiar `DB_PORT`/`APP_PORT` en `.env`.
- **La app arranca pero sin datos** → los datos semilla solo se cargan con el perfil
  `dev` (Compose ya lo configura) y solo si la base está vacía; `docker compose down -v`
  y volver a levantar para re-sembrar.
- **Flyway falla tras cambios de esquema** → `docker compose down -v` (borra datos
  locales) y levantar de nuevo.
