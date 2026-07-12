# Diagramas

Diagramas del proyecto en formato **Mermaid** (se renderizan automáticamente en GitHub
y en editores compatibles). Reflejan el estado real del código de la **Fase 1** (dominio) y
la **Fase 2** (modelo de datos), y anotan qué partes corresponden a fases posteriores.

| # | Diagrama | Qué explica |
|---|---|---|
| 01 | [Máquina de estados](01-maquina-estados.md) | Estados del préstamo y transiciones legales |
| 02 | [Flujo del proceso](02-flujo-proceso-prestamo.md) | Recorrido end-to-end de una solicitud + evaluación asíncrona |
| 03 | [Arquitectura de capas](03-arquitectura-capas.md) | Monolito modular y regla de dependencias hacia el dominio |
| 04 | [Modelo de dominio](04-modelo-dominio.md) | Jerarquías de clases (Cliente, Préstamo, Estado, Cálculo) |
| 05 | [Motor de scoring](05-motor-scoring.md) | Reglas ponderadas, brackets y decisión |
| 06 | [Modelo de datos (ER)](06-modelo-datos-er.md) | Esquema relacional de la Fase 2 (tablas, FKs, herencia JOINED) |

## Cómo verlos

- **GitHub:** se renderizan solos al abrir cada `.md`.
- **VS Code:** con la extensión *Markdown Preview Mermaid Support* (o similar), abrir la
  vista previa (`Ctrl+Shift+V`).
- **En línea:** pegar el bloque ` ```mermaid ` en <https://mermaid.live>.

> Al modificar el dominio, actualizar el diagrama correspondiente para que no quede desfasado.
