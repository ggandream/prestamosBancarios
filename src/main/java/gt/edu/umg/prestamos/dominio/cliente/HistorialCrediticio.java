package gt.edu.umg.prestamos.dominio.cliente;

/**
 * Historial crediticio del cliente. Es un <strong>campo almacenado</strong> (cargado
 * manualmente o por seed); nunca se consulta a un buró externo. Ver Sección 5 del
 * CLAUDE.md: el scoring es 100% determinista y basado en reglas.
 */
public enum HistorialCrediticio {
    BUENO,
    REGULAR,
    MALO
}
