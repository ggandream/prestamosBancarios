import type { EstadoPrestamo } from "./api";

const quetzales = new Intl.NumberFormat("es-GT", {
  style: "currency",
  currency: "GTQ",
  minimumFractionDigits: 2,
});

/** Formatea un monto como quetzales (Q1,234.56). */
export function dinero(valor: number): string {
  return quetzales.format(valor);
}

/** Formatea una fecha ISO (yyyy-MM-dd) en formato local corto. */
export function fecha(iso: string): string {
  return new Date(`${iso}T00:00:00`).toLocaleDateString("es-GT", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

/** Color de badge Mantine para cada estado del préstamo. */
export function colorEstado(estado: EstadoPrestamo): string {
  switch (estado) {
    case "Borrador":
      return "gray";
    case "EnEvaluacion":
      return "yellow";
    case "Aprobado":
      return "pino";
    case "Rechazado":
      return "red";
    case "Desembolsado":
      return "blue";
    case "Pagado":
      return "teal";
    case "EnMora":
      return "orange";
  }
}

/** Etiqueta legible para cada estado. */
export function etiquetaEstado(estado: EstadoPrestamo): string {
  switch (estado) {
    case "EnEvaluacion":
      return "En evaluación";
    case "EnMora":
      return "En mora";
    default:
      return estado;
  }
}

/** Los estados transitorios se animan mientras el listener asíncrono trabaja. */
export function estadoTransitorio(estado: EstadoPrestamo): boolean {
  return estado === "Borrador" || estado === "EnEvaluacion";
}
