# Modelo de datos (diagrama entidad-relación)

Esquema relacional de la **Fase 2** (persistencia), tal como lo crea la migración
`src/main/resources/db/migration/V1__esquema_inicial.sql`. Refleja la estrategia de
herencia **`JOINED`** (una tabla base + una tabla por subtipo que comparte la PK y la
referencia por FK) y el estado del préstamo **aplanado** en columnas.

```mermaid
erDiagram
    CLIENTE ||--o| CLIENTE_INDIVIDUAL : "es-un"
    CLIENTE ||--o| CLIENTE_EMPRESARIAL : "es-un"
    CLIENTE ||--o{ PRESTAMO : "solicita"
    PRESTAMO ||--o| PRESTAMO_PERSONAL : "es-un"
    PRESTAMO ||--o| PRESTAMO_HIPOTECARIO : "es-un"
    PRESTAMO ||--o| PRESTAMO_AUTOMOTRIZ : "es-un"
    PRESTAMO ||--o{ CUOTA : "tiene"

    CLIENTE {
        uuid id PK
        varchar tipo_cliente "discriminador JOINED"
        varchar nombre
        varchar documento UK
        varchar email
        date fecha_registro
        varchar historial "BUENO / REGULAR / MALO"
    }

    CLIENTE_INDIVIDUAL {
        uuid id PK,FK
        numeric salario_mensual "positivo"
        varchar tipo_empleo "FORMAL / INDEPENDIENTE / INFORMAL"
        integer antiguedad_laboral "no negativo"
    }

    CLIENTE_EMPRESARIAL {
        uuid id PK,FK
        numeric facturacion_anual "positivo"
        varchar nit UK
        varchar sector "COMERCIO / INDUSTRIA / SERVICIOS / AGRICOLA / CONSTRUCCION"
        integer antiguedad_nit "no negativo"
    }

    PRESTAMO {
        uuid id PK
        varchar tipo_prestamo "discriminador JOINED"
        uuid cliente_id FK
        numeric monto "positivo"
        integer plazo_meses "6..360"
        numeric tasa_anual "positivo (fracción)"
        date fecha_solicitud
        varchar estado_tipo "discriminador de estado"
        timestamp estado_fecha "nullable"
        varchar estado_texto "nullable (evaluador / motivo)"
        integer estado_score "nullable (Aprobado)"
        numeric estado_monto "nullable (Desembolsado / EnMora)"
        integer estado_dias_atraso "nullable (EnMora)"
    }

    PRESTAMO_PERSONAL {
        uuid id PK,FK
    }

    PRESTAMO_HIPOTECARIO {
        uuid id PK,FK
        varchar descripcion_garantia
        numeric avaluo "positivo"
    }

    PRESTAMO_AUTOMOTRIZ {
        uuid id PK,FK
        varchar vehiculo
        numeric depreciacion_anual "positivo"
    }

    CUOTA {
        bigint id PK
        uuid prestamo_id FK
        integer numero "positivo, UK(prestamo_id, numero)"
        date fecha_pago
        numeric capital
        numeric interes
        numeric total
    }
```

## Notas de diseño

- **Herencia `JOINED`.** `CLIENTE_INDIVIDUAL`/`CLIENTE_EMPRESARIAL` y las tres tablas de
  préstamo comparten la clave primaria con su tabla base (`id` es a la vez **PK y FK**). Una
  fila base tiene **como máximo una** fila de subtipo (relación `||--o|`), determinada por el
  discriminador (`tipo_cliente` / `tipo_prestamo`). Esto permite `NOT NULL` real en los
  campos propios de cada subtipo.
- **Estado del préstamo aplanado.** En vez de una tabla aparte, el estado vive como columnas
  en `PRESTAMO`: el discriminador `estado_tipo` + columnas de datos nullables que, en
  conjunto, cubren los 7 records de `EstadoPrestamo`. El `EstadoMapper` reconstruye el record
  exacto al leer. Ver [01-maquina-estados.md](01-maquina-estados.md).
- **Plan de pagos.** `CUOTA` guarda el plan (snapshot determinista) con una restricción
  `UNIQUE (prestamo_id, numero)` para que no se repitan números de cuota dentro de un
  préstamo.
- **Restricciones de integridad.** FKs entre subtipos y base, `UNIQUE` en `documento` y
  `nit`, y `CHECK` de montos positivos, rango de plazo y dominios de enumerados (ver el SQL
  de la migración y [docs/fase2-persistencia.md](../docs/fase2-persistencia.md)).
- **Correspondencia con el dominio.** Este diagrama es el reflejo *relacional* del
  [modelo de dominio](04-modelo-dominio.md); las entidades JPA de `persistencia.entidad`
  se traducen a las clases de dominio mediante mapeadores manuales.
