# Flujo del proceso de préstamo (end-to-end)

Recorrido completo de una solicitud, desde que se registra el cliente hasta que se
genera el plan de pagos. Cada bloque indica **en qué fase del roadmap** se implementa,
para distinguir lo que ya existe (Fase 1: dominio) de lo que viene después.

```mermaid
flowchart TD
    A([Registrar cliente]) --> B[Crear solicitud de préstamo<br/>estado inicial: Borrador]
    B --> C[Enviar a evaluación<br/>Borrador → EnEvaluacion]
    C --> D{{MotorScoring<br/>4 reglas ponderadas}}
    D --> E{"score >= 60?"}
    E -->|Sí| F[Aprobado]
    E -->|No| G[Rechazado]
    F --> H[Desembolsar<br/>Aprobado → Desembolsado]
    H --> I[Generar plan de pagos<br/>CalculadoraInteres]
    I --> J{¿Cuotas al día?}
    J -->|Sí| K[Pagado]
    J -->|No| L[EnMora]
    L -->|regulariza| K

    G --> M([Fin: solicitud cerrada])
    K --> N([Fin: préstamo saldado])

    classDef dominio fill:#dbeafe,stroke:#2563eb,color:#1e3a8a;
    classDef futuro fill:#f1f5f9,stroke:#94a3b8,color:#334155,stroke-dasharray: 4 3;
    class B,C,D,E,F,G,H,I,J,K,L dominio;
    class A,M,N futuro;
```

> 🔵 **Azul** = lógica de dominio ya implementada (Fase 1).
> ⬜ **Gris punteado** = orquestación en capas superiores (servicios/API/eventos, Fases 3–4).

## Evaluación asíncrona (objetivo de la Fase 4)

Cuando se implemente el componente asíncrono, la evaluación no bloqueará la respuesta
HTTP: la API responde `202 Accepted` y un listener evalúa en segundo plano.

```mermaid
sequenceDiagram
    actor Cliente as Usuario/API Client
    participant API as SolicitudController
    participant Svc as SolicitudService
    participant Bus as ApplicationEventPublisher
    participant Lis as ListenerEvaluacion @Async
    participant Motor as MotorScoring

    Cliente->>API: POST /api/solicitudes
    API->>Svc: crear(solicitud)
    Svc->>Svc: guardar en estado Borrador
    Svc-)Bus: publica EventoSolicitudCreada
    API-->>Cliente: 202 Accepted (id)

    Bus-)Lis: EventoSolicitudCreada
    Lis->>Motor: evaluar(cliente, prestamo)
    Motor-->>Lis: ResultadoEvaluacion (score, decisión)
    Lis->>Lis: EnEvaluacion → Aprobado / Rechazado
    Lis-)Bus: publica EventoEvaluacionCompletada
```

> Nota: en la Fase 1 solo existe el **dominio** (`MotorScoring`, estados, calculadoras).
> Los servicios, controladores y listeners de este diagrama corresponden a las Fases 3 y 4.
