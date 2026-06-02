# Plataforma de Gestión y Evaluación de Préstamos

Sistema de gestión y evaluación de solicitudes de préstamos para una institución financiera. Registra clientes, captura solicitudes, las evalúa mediante un motor de scoring basado en reglas, genera planes de amortización y procesa operaciones de forma asíncrona.

> Proyecto académico — Programación Avanzada en Java. El objetivo es demostrar dominio de Java moderno, SOLID, patrones de diseño y prácticas DevOps.

---

## Equipo

| Integrante | Rol |
|---|---|
| Andrea Garrido | Persistencia (JPA, modelo de datos, migraciones) |
| Luis Humberto Ruiz | Backend (servicios, scoring, API REST) |
| Luis Renato Granados | Dev/Ops y seguridad (Docker, CI/CD, configuración) |

---

## Requisitos

- Java 25+
- Docker y Docker Compose
- PostgreSQL 15+ (si se ejecuta sin Docker)
- Maven 3.9+

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
