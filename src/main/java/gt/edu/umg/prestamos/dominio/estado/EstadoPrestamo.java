package gt.edu.umg.prestamos.dominio.estado;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Estado de un préstamo, modelado como una interfaz sellada con un conjunto
 * <strong>cerrado</strong> de records. Permite exhaustividad total en los
 * {@code switch} con pattern matching, sin rama {@code default}.
 */
public sealed interface EstadoPrestamo
        permits EstadoPrestamo.Borrador, EstadoPrestamo.EnEvaluacion, EstadoPrestamo.Aprobado,
                EstadoPrestamo.Rechazado, EstadoPrestamo.Desembolsado, EstadoPrestamo.Pagado,
                EstadoPrestamo.EnMora {

    record Borrador(LocalDateTime fechaCreacion) implements EstadoPrestamo {}

    record EnEvaluacion(LocalDateTime fechaInicio, String evaluador) implements EstadoPrestamo {}

    record Aprobado(LocalDateTime fechaAprobacion, int scoreObtenido) implements EstadoPrestamo {}

    record Rechazado(LocalDateTime fechaRechazo, String motivo) implements EstadoPrestamo {}

    record Desembolsado(LocalDateTime fecha, BigDecimal montoDesembolsado) implements EstadoPrestamo {}

    record Pagado(LocalDateTime fechaUltimoPago) implements EstadoPrestamo {}

    record EnMora(int diasAtraso, BigDecimal montoVencido) implements EstadoPrestamo {}
}
