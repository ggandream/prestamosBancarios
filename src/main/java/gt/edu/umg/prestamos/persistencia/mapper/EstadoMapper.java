package gt.edu.umg.prestamos.persistencia.mapper;

import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Aprobado;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Borrador;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Desembolsado;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.EnEvaluacion;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.EnMora;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Pagado;
import gt.edu.umg.prestamos.dominio.estado.EstadoPrestamo.Rechazado;
import gt.edu.umg.prestamos.persistencia.entidad.EstadoEmbeddable;
import gt.edu.umg.prestamos.persistencia.entidad.TipoEstado;
import org.springframework.stereotype.Component;

/**
 * Traduce el estado de un préstamo entre el sealed interface del dominio
 * ({@link EstadoPrestamo}) y su representación aplanada en columnas
 * ({@link EstadoEmbeddable}).
 *
 * <p>De ida, un {@code switch} exhaustivo con pattern matching (sin {@code default},
 * garantizado por el sealed interface) reparte cada campo del record en la columna
 * genérica que le corresponde. De vuelta, un {@code switch} sobre el discriminador
 * {@link TipoEstado} reconstruye el record exacto.
 */
@Component
public class EstadoMapper {

    public EstadoEmbeddable aEmbeddable(EstadoPrestamo estado) {
        return switch (estado) {
            case Borrador b ->
                    new EstadoEmbeddable(TipoEstado.BORRADOR, b.fechaCreacion(), null, null, null, null);
            case EnEvaluacion e ->
                    new EstadoEmbeddable(TipoEstado.EN_EVALUACION, e.fechaInicio(), e.evaluador(), null, null, null);
            case Aprobado a ->
                    new EstadoEmbeddable(TipoEstado.APROBADO, a.fechaAprobacion(), null, a.scoreObtenido(), null, null);
            case Rechazado r ->
                    new EstadoEmbeddable(TipoEstado.RECHAZADO, r.fechaRechazo(), r.motivo(), null, null, null);
            case Desembolsado d ->
                    new EstadoEmbeddable(TipoEstado.DESEMBOLSADO, d.fecha(), null, null, d.montoDesembolsado(), null);
            case Pagado p ->
                    new EstadoEmbeddable(TipoEstado.PAGADO, p.fechaUltimoPago(), null, null, null, null);
            case EnMora m ->
                    new EstadoEmbeddable(TipoEstado.EN_MORA, null, null, null, m.montoVencido(), m.diasAtraso());
        };
    }

    public EstadoPrestamo aDominio(EstadoEmbeddable e) {
        return switch (e.getTipo()) {
            case BORRADOR -> new Borrador(e.getFecha());
            case EN_EVALUACION -> new EnEvaluacion(e.getFecha(), e.getTexto());
            case APROBADO -> new Aprobado(e.getFecha(), e.getScore());
            case RECHAZADO -> new Rechazado(e.getFecha(), e.getTexto());
            case DESEMBOLSADO -> new Desembolsado(e.getFecha(), e.getMonto());
            case PAGADO -> new Pagado(e.getFecha());
            case EN_MORA -> new EnMora(e.getDiasAtraso(), e.getMonto());
        };
    }
}
