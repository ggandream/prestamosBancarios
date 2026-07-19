package gt.edu.umg.prestamos.servicio;

import gt.edu.umg.prestamos.dominio.analisis.AnalizadorCartera;
import gt.edu.umg.prestamos.dominio.analisis.ResumenCartera;
import gt.edu.umg.prestamos.persistencia.adaptador.PrestamoRepositorioJpa;
import org.springframework.stereotype.Service;

/**
 * Servicio de reportería: ejecuta el pipeline puro de {@link AnalizadorCartera}
 * (Stream API, migrado de la app de consola) sobre los préstamos persistidos.
 */
@Service
public class ReporteService {

    private final PrestamoRepositorioJpa prestamos;
    private final AnalizadorCartera analizador = new AnalizadorCartera();

    public ReporteService(PrestamoRepositorioJpa prestamos) {
        this.prestamos = prestamos;
    }

    /** Resumen de la cartera completa según los datos reales de la base. */
    public ResumenCartera resumenCartera() {
        return analizador.analizar(prestamos.buscarTodos());
    }
}
