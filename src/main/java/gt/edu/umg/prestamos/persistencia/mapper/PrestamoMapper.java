package gt.edu.umg.prestamos.persistencia.mapper;

import gt.edu.umg.prestamos.dominio.prestamo.Prestamo;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoAutomotriz;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoHipotecario;
import gt.edu.umg.prestamos.dominio.prestamo.PrestamoPersonal;
import gt.edu.umg.prestamos.persistencia.entidad.ClienteEntity;
import gt.edu.umg.prestamos.persistencia.entidad.EstadoEmbeddable;
import gt.edu.umg.prestamos.persistencia.entidad.PrestamoAutomotrizEntity;
import gt.edu.umg.prestamos.persistencia.entidad.PrestamoEntity;
import gt.edu.umg.prestamos.persistencia.entidad.PrestamoHipotecarioEntity;
import gt.edu.umg.prestamos.persistencia.entidad.PrestamoPersonalEntity;
import org.springframework.stereotype.Component;

/**
 * Mapeador manual {@code Prestamo} (dominio) &lt;-&gt; {@code PrestamoEntity} (JPA).
 *
 * <p>Compone {@link EstadoMapper} (estado embebido) y {@link CuotaMapper} (plan de pagos).
 * El {@code ClienteEntity} asociado se recibe ya resuelto por el adaptador de repositorio
 * (referencia gestionada por el {@code EntityManager}) para no crear entidades transitorias
 * ni cascadear la persistencia del cliente al guardar un préstamo.
 */
@Component
public class PrestamoMapper {

    private final EstadoMapper estadoMapper;
    private final CuotaMapper cuotaMapper;
    private final ClienteMapper clienteMapper;

    public PrestamoMapper(EstadoMapper estadoMapper, CuotaMapper cuotaMapper, ClienteMapper clienteMapper) {
        this.estadoMapper = estadoMapper;
        this.cuotaMapper = cuotaMapper;
        this.clienteMapper = clienteMapper;
    }

    /**
     * Construye la entidad JPA a partir del préstamo de dominio. Genera y adjunta el plan
     * de cuotas (determinista) como snapshot persistido.
     *
     * @param dominio        préstamo de dominio a persistir
     * @param clienteEntity  entidad de cliente ya gestionada (misma identidad que el cliente del dominio)
     */
    public PrestamoEntity aEntidad(Prestamo dominio, ClienteEntity clienteEntity) {
        EstadoEmbeddable estado = estadoMapper.aEmbeddable(dominio.getEstado());
        PrestamoEntity entity = switch (dominio) {
            case PrestamoPersonal p -> new PrestamoPersonalEntity(
                    p.getId(), clienteEntity, p.getMonto(), p.getPlazoMeses(),
                    p.getTasaAnual(), p.getFechaSolicitud(), estado);
            case PrestamoHipotecario h -> new PrestamoHipotecarioEntity(
                    h.getId(), clienteEntity, h.getMonto(), h.getPlazoMeses(),
                    h.getTasaAnual(), h.getFechaSolicitud(), estado,
                    h.getDescripcionGarantia(), h.getAvaluo());
            case PrestamoAutomotriz a -> new PrestamoAutomotrizEntity(
                    a.getId(), clienteEntity, a.getMonto(), a.getPlazoMeses(),
                    a.getTasaAnual(), a.getFechaSolicitud(), estado,
                    a.getVehiculo(), a.getDepreciacionAnual());
            default -> throw new IllegalArgumentException(
                    "Subtipo de Prestamo no soportado: " + dominio.getClass().getName());
        };
        dominio.generarPlanPagos().forEach(cuota -> entity.agregarCuota(cuotaMapper.aEntidad(cuota)));
        return entity;
    }

    /**
     * Rehidrata el préstamo de dominio desde la entidad. El estado persistido se restaura
     * con {@code Prestamo#restaurarEstado} (sin re-validar la máquina de estados). El plan
     * de cuotas no se inyecta al dominio: éste lo regenera de forma determinista bajo demanda.
     */
    public Prestamo aDominio(PrestamoEntity entity) {
        var cliente = clienteMapper.aDominio(entity.getCliente());
        Prestamo dominio = switch (entity) {
            case PrestamoPersonalEntity p -> new PrestamoPersonal(
                    p.getId(), cliente, p.getMonto(), p.getPlazoMeses(),
                    p.getTasaAnual(), p.getFechaSolicitud());
            case PrestamoHipotecarioEntity h -> new PrestamoHipotecario(
                    h.getId(), cliente, h.getMonto(), h.getPlazoMeses(),
                    h.getTasaAnual(), h.getFechaSolicitud(),
                    h.getDescripcionGarantia(), h.getAvaluo());
            case PrestamoAutomotrizEntity a -> new PrestamoAutomotriz(
                    a.getId(), cliente, a.getMonto(), a.getPlazoMeses(),
                    a.getTasaAnual(), a.getFechaSolicitud(),
                    a.getVehiculo(), a.getDepreciacionAnual());
            default -> throw new IllegalArgumentException(
                    "Subtipo de PrestamoEntity no soportado: " + entity.getClass().getName());
        };
        dominio.restaurarEstado(estadoMapper.aDominio(entity.getEstado()));
        return dominio;
    }
}
