package gt.edu.umg.prestamos.api.dto;

import gt.edu.umg.prestamos.dominio.prestamo.Cuota;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Una cuota del plan de amortización, tal como la expone la API. */
public record CuotaDTO(
        int numero,
        LocalDate fechaPago,
        BigDecimal capital,
        BigDecimal interes,
        BigDecimal total) {

    /** Mapea la cuota de dominio a su representación de API. */
    public static CuotaDTO desde(Cuota cuota) {
        return new CuotaDTO(cuota.numero(), cuota.fechaPago(), cuota.capital(),
                cuota.interes(), cuota.total());
    }
}
