# Modelo de dominio (diagrama de clases)

Jerarquías del Entregable 1 tal como están implementadas en `dominio` (Fase 1). Las
jerarquías son **cerradas**: no se agregan más tipos de cliente, préstamo, estado,
regla ni calculadora que los aquí mostrados.

```mermaid
classDiagram
    direction TB

    class Cliente {
        <<abstract>>
        +UUID id
        +String nombre
        +String documento
        +String email
        +HistorialCrediticio historial
        +getCapacidadPago() BigDecimal*
        +getIngresoMensual() BigDecimal*
        +getAntiguedadAnios() int*
        +calcularScoreBase() int
    }
    class ClienteIndividual {
        +BigDecimal salarioMensual
        +TipoEmpleo tipoEmpleo
        +int antiguedadLaboral
    }
    class ClienteEmpresarial {
        +BigDecimal facturacionAnual
        +String nit
        +SectorIndustria sector
        +int antiguedadNit
    }
    Cliente <|-- ClienteIndividual
    Cliente <|-- ClienteEmpresarial

    class Prestamo {
        <<abstract>>
        +UUID id
        +BigDecimal monto
        +int plazoMeses
        +BigDecimal tasaAnual
        +EstadoPrestamo estado
        +calcularCuota() BigDecimal
        +calcularInteres() BigDecimal
        +generarPlanPagos() List~Cuota~
        +cambiarEstado(EstadoPrestamo)
        +descripcionEstado() String
        #calculadoraPorDefecto() CalculadoraInteres*
    }
    class PrestamoPersonal
    class PrestamoHipotecario {
        +String descripcionGarantia
        +BigDecimal avaluo
    }
    class PrestamoAutomotriz {
        +String vehiculo
        +BigDecimal depreciacionAnual
    }
    Prestamo <|-- PrestamoPersonal
    Prestamo <|-- PrestamoHipotecario
    Prestamo <|-- PrestamoAutomotriz

    class Cuota {
        <<record>>
        +int numero
        +LocalDate fechaPago
        +BigDecimal capital
        +BigDecimal interes
        +BigDecimal total
    }

    class EstadoPrestamo {
        <<sealed interface>>
    }
    note for EstadoPrestamo "7 records permitidos: Borrador, EnEvaluacion, Aprobado, Rechazado, Desembolsado, Pagado, EnMora"

    class CalculadoraInteres {
        <<interface>>
        +calcular(monto, plazo, tasa, fecha) List~Cuota~
    }
    class MetodoFrances {
        +nombre() String
    }
    class MetodoAleman {
        +nombre() String
    }
    CalculadoraInteres <|.. MetodoFrances
    CalculadoraInteres <|.. MetodoAleman

    Prestamo o--> "1" Cliente : cliente
    Prestamo --> "1" EstadoPrestamo : estado
    Prestamo ..> CalculadoraInteres : usa (Strategy)
    Prestamo ..> Cuota : genera
    CalculadoraInteres ..> Cuota : produce
    PrestamoPersonal ..> MetodoFrances
    PrestamoHipotecario ..> MetodoFrances
    PrestamoAutomotriz ..> MetodoAleman
```

## Notas de diseño

- `Cliente` y `Prestamo` son **abstractos**; el comportamiento específico (capacidad de
  pago, método de amortización) se resuelve por polimorfismo.
- El método de amortización es un **Strategy** (`CalculadoraInteres`): Personal e
  Hipotecario usan francés (cuota fija); Automotriz usa alemán (capital constante,
  acorde a la depreciación del bien).
- Todo monto es `BigDecimal` con escala 2 y `RoundingMode.HALF_UP`.
- El scoring tiene su propio diagrama: ver [05-motor-scoring.md](05-motor-scoring.md).
