/**
 * Cliente tipado de la API REST del backend (fases 3-4). Las rutas relativas
 * pasan por el proxy de Vite hacia http://localhost:8080.
 */

export interface ClienteDTO {
  id: string;
  tipo: "INDIVIDUAL" | "EMPRESARIAL";
  nombre: string;
  documento: string;
  email: string;
  fechaRegistro: string;
  historial: "BUENO" | "REGULAR" | "MALO";
  ingresoMensual: number;
  capacidadPago: number;
}

export interface RegistroCliente {
  tipo: "INDIVIDUAL" | "EMPRESARIAL";
  nombre: string;
  documento: string;
  email: string;
  historial: string;
  salarioMensual?: number;
  tipoEmpleo?: string;
  antiguedadLaboral?: number;
  facturacionAnual?: number;
  nit?: string;
  sector?: string;
  antiguedadNit?: number;
}

export type EstadoPrestamo =
  | "Borrador"
  | "EnEvaluacion"
  | "Aprobado"
  | "Rechazado"
  | "Desembolsado"
  | "Pagado"
  | "EnMora";

export interface PrestamoDTO {
  id: string;
  clienteId: string;
  tipo: "PERSONAL" | "HIPOTECARIO" | "AUTOMOTRIZ";
  monto: number;
  plazoMeses: number;
  tasaAnual: number;
  fechaSolicitud: string;
  estado: EstadoPrestamo;
  descripcionEstado: string;
  cuotaEstimada: number;
}

export interface NuevaSolicitud {
  clienteId: string;
  tipoPrestamo: "PERSONAL" | "HIPOTECARIO" | "AUTOMOTRIZ";
  monto: number;
  plazoMeses: number;
  tasaAnual: number;
  descripcionGarantia?: string;
  avaluo?: number;
  vehiculo?: string;
  depreciacionAnual?: number;
}

export interface CuotaDTO {
  numero: number;
  fechaPago: string;
  capital: number;
  interes: number;
  total: number;
}

export interface PlanPagosDTO {
  prestamoId: string;
  metodo: string;
  monto: number;
  plazoMeses: number;
  totalIntereses: number;
  totalPagar: number;
  cuotas: CuotaDTO[];
}

export interface ResumenCarteraDTO {
  totalPrestamos: number;
  montoTotal: number;
  montoPromedio: number;
  montoMinimo: number;
  montoMaximo: number;
  indiceMora: number;
  conteoPorTipo: Record<string, number>;
  montoPorTipo: Record<string, number>;
  conteoPorRiesgo: Record<string, number>;
  conclusiones: string[];
}

async function http<T>(ruta: string, init?: RequestInit): Promise<T> {
  const respuesta = await fetch(ruta, {
    headers: { "Content-Type": "application/json" },
    ...init,
  });
  if (!respuesta.ok) {
    const cuerpo = await respuesta.json().catch(() => null);
    throw new Error(cuerpo?.mensaje ?? `Error HTTP ${respuesta.status}`);
  }
  return respuesta.json() as Promise<T>;
}

export const api = {
  listarClientes: () => http<ClienteDTO[]>("/api/clientes"),
  registrarCliente: (datos: RegistroCliente) =>
    http<ClienteDTO>("/api/clientes", {
      method: "POST",
      body: JSON.stringify(datos),
    }),
  listarPrestamos: () => http<PrestamoDTO[]>("/api/prestamos"),
  crearSolicitud: (datos: NuevaSolicitud) =>
    http<PrestamoDTO>("/api/solicitudes", {
      method: "POST",
      body: JSON.stringify(datos),
    }),
  desembolsar: (prestamoId: string) =>
    http<PrestamoDTO>(`/api/prestamos/${prestamoId}/desembolsar`, {
      method: "POST",
    }),
  planPagos: (prestamoId: string, metodo?: string) =>
    http<PlanPagosDTO>(
      `/api/prestamos/${prestamoId}/plan-pagos${metodo ? `?metodo=${metodo}` : ""}`,
    ),
  reporteCartera: () => http<ResumenCarteraDTO>("/api/reportes/cartera"),
};
