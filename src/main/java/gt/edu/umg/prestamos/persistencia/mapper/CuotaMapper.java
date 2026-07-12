package gt.edu.umg.prestamos.persistencia.mapper;

import gt.edu.umg.prestamos.dominio.prestamo.Cuota;
import gt.edu.umg.prestamos.persistencia.entidad.CuotaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapeador manual {@code Cuota} (dominio, record inmutable) &lt;-&gt; {@code CuotaEntity} (JPA).
 * La FK al préstamo la establece {@code PrestamoEntity#agregarCuota}, no este mapper.
 */
@Component
public class CuotaMapper {

    public CuotaEntity aEntidad(Cuota cuota) {
        return new CuotaEntity(
                cuota.numero(), cuota.fechaPago(), cuota.capital(), cuota.interes(), cuota.total());
    }

    public Cuota aDominio(CuotaEntity entity) {
        return new Cuota(
                entity.getNumero(), entity.getFechaPago(),
                entity.getCapital(), entity.getInteres(), entity.getTotal());
    }
}
