import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Alert,
  Badge,
  Button,
  Card,
  Group,
  Loader,
  Modal,
  NumberInput,
  SegmentedControl,
  Select,
  Stack,
  Table,
  Text,
  TextInput,
  Title,
  Tooltip,
} from "@mantine/core";
import {
  api,
  type ClienteDTO,
  type PlanPagosDTO,
  type PrestamoDTO,
} from "../api";
import {
  colorEstado,
  dinero,
  estadoTransitorio,
  etiquetaEstado,
  fecha,
} from "../formato";

/** Alta de solicitud: al enviarse, el backend responde 202 y evalúa en background. */
function FormularioSolicitud({
  abierto,
  clientes,
  alCerrar,
  alCrear,
}: {
  abierto: boolean;
  clientes: ClienteDTO[];
  alCerrar: () => void;
  alCrear: () => void;
}) {
  const [clienteId, setClienteId] = useState<string | null>(null);
  const [tipo, setTipo] = useState<"PERSONAL" | "HIPOTECARIO" | "AUTOMOTRIZ">("PERSONAL");
  const [monto, setMonto] = useState<number | string>(50000);
  const [plazo, setPlazo] = useState<number | string>(60);
  const [tasaPorcentaje, setTasaPorcentaje] = useState<number | string>(12);
  const [garantia, setGarantia] = useState("");
  const [avaluo, setAvaluo] = useState<number | string>(300000);
  const [vehiculo, setVehiculo] = useState("");
  const [depreciacionPorcentaje, setDepreciacionPorcentaje] = useState<number | string>(15);
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const crear = async () => {
    if (!clienteId) {
      setError("Seleccione un cliente");
      return;
    }
    setEnviando(true);
    setError(null);
    try {
      await api.crearSolicitud({
        clienteId,
        tipoPrestamo: tipo,
        monto: Number(monto),
        plazoMeses: Number(plazo),
        tasaAnual: Number(tasaPorcentaje) / 100,
        ...(tipo === "HIPOTECARIO" && {
          descripcionGarantia: garantia,
          avaluo: Number(avaluo),
        }),
        ...(tipo === "AUTOMOTRIZ" && {
          vehiculo,
          depreciacionAnual: Number(depreciacionPorcentaje) / 100,
        }),
      });
      alCrear();
      alCerrar();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Modal opened={abierto} onClose={alCerrar} title="Nueva solicitud de préstamo" size="md">
      <Stack gap="sm">
        <Select
          label="Cliente"
          placeholder="Buscar cliente"
          searchable
          data={clientes.map((c) => ({
            value: c.id,
            label: `${c.nombre} · ${c.documento}`,
          }))}
          value={clienteId}
          onChange={setClienteId}
          required
        />
        <SegmentedControl
          fullWidth
          value={tipo}
          onChange={(v) => setTipo(v as typeof tipo)}
          data={[
            { label: "Personal", value: "PERSONAL" },
            { label: "Hipotecario", value: "HIPOTECARIO" },
            { label: "Automotriz", value: "AUTOMOTRIZ" },
          ]}
        />
        <Group grow>
          <NumberInput label="Monto" value={monto} onChange={setMonto} min={1} thousandSeparator="," prefix="Q" />
          <NumberInput label="Plazo (meses)" description="entre 6 y 360" value={plazo} onChange={setPlazo} min={6} max={360} />
          <NumberInput label="Tasa anual" value={tasaPorcentaje} onChange={setTasaPorcentaje} min={0.1} suffix="%" decimalScale={2} />
        </Group>

        {tipo === "HIPOTECARIO" && (
          <Group grow>
            <TextInput label="Garantía" placeholder="Casa en zona 10" value={garantia} onChange={(e) => setGarantia(e.currentTarget.value)} required />
            <NumberInput label="Avalúo" description="monto ≤ 80% del avalúo" value={avaluo} onChange={setAvaluo} min={1} thousandSeparator="," prefix="Q" />
          </Group>
        )}
        {tipo === "AUTOMOTRIZ" && (
          <Group grow>
            <TextInput label="Vehículo" placeholder="Camión Hino 300, 2024" value={vehiculo} onChange={(e) => setVehiculo(e.currentTarget.value)} required />
            <NumberInput label="Depreciación anual" value={depreciacionPorcentaje} onChange={setDepreciacionPorcentaje} min={0.1} suffix="%" decimalScale={2} />
          </Group>
        )}

        {error && <Alert color="red">{error}</Alert>}
        <Group justify="end" mt="xs">
          <Button variant="default" onClick={alCerrar}>Cancelar</Button>
          <Button onClick={crear} loading={enviando}>Enviar solicitud</Button>
        </Group>
      </Stack>
    </Modal>
  );
}

/** Plan de amortización con selector de método (Factory en el backend). */
function ModalPlanPagos({
  prestamo,
  alCerrar,
}: {
  prestamo: PrestamoDTO | null;
  alCerrar: () => void;
}) {
  const [metodo, setMetodo] = useState<string>("");
  const [plan, setPlan] = useState<PlanPagosDTO | null>(null);

  useEffect(() => {
    if (!prestamo) return;
    setPlan(null);
    api.planPagos(prestamo.id, metodo || undefined).then(setPlan).catch(() => setPlan(null));
  }, [prestamo, metodo]);

  return (
    <Modal
      opened={prestamo !== null}
      onClose={() => { setMetodo(""); alCerrar(); }}
      title="Plan de amortización"
      size="lg"
    >
      <Stack gap="sm">
        <Group justify="space-between" align="end">
          <SegmentedControl
            value={metodo}
            onChange={setMetodo}
            data={[
              { label: "Del producto", value: "" },
              { label: "Francés", value: "FRANCES" },
              { label: "Alemán", value: "ALEMAN" },
            ]}
          />
          {plan && (
            <Text size="sm" c="dimmed">
              Método <b>{plan.metodo}</b> · Intereses{" "}
              <span className="dinero">{dinero(plan.totalIntereses)}</span> · Total{" "}
              <span className="dinero">{dinero(plan.totalPagar)}</span>
            </Text>
          )}
        </Group>

        {!plan ? (
          <Loader mx="auto" my="lg" />
        ) : (
          <Table.ScrollContainer minWidth={480} mah={380}>
            <Table stickyHeader verticalSpacing="xs" striped>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>#</Table.Th>
                  <Table.Th>Fecha</Table.Th>
                  <Table.Th ta="right">Capital</Table.Th>
                  <Table.Th ta="right">Interés</Table.Th>
                  <Table.Th ta="right">Cuota</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {plan.cuotas.map((c) => (
                  <Table.Tr key={c.numero}>
                    <Table.Td>{c.numero}</Table.Td>
                    <Table.Td>{fecha(c.fechaPago)}</Table.Td>
                    <Table.Td ta="right"><span className="dinero">{dinero(c.capital)}</span></Table.Td>
                    <Table.Td ta="right"><span className="dinero">{dinero(c.interes)}</span></Table.Td>
                    <Table.Td ta="right"><span className="dinero">{dinero(c.total)}</span></Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        )}
      </Stack>
    </Modal>
  );
}

/**
 * Listado de préstamos. Mientras exista una solicitud en estado transitorio
 * (Borrador/EnEvaluacion) la tabla se refresca sola cada 1.5 s: así la demo
 * muestra en vivo la evaluación asíncrona del backend (202 → Aprobado/Rechazado).
 */
export function PanelPrestamos() {
  const [prestamos, setPrestamos] = useState<PrestamoDTO[] | null>(null);
  const [clientes, setClientes] = useState<ClienteDTO[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const [modalSolicitud, setModalSolicitud] = useState(false);
  const [planDe, setPlanDe] = useState<PrestamoDTO | null>(null);

  const cargar = useCallback(() => {
    api.listarPrestamos().then(setPrestamos).catch((e) => setError(e.message));
    api.listarClientes().then(setClientes).catch(() => undefined);
  }, []);

  useEffect(cargar, [cargar]);

  const hayTransitorios = useMemo(
    () => (prestamos ?? []).some((p) => estadoTransitorio(p.estado)),
    [prestamos],
  );

  useEffect(() => {
    if (!hayTransitorios) return;
    const temporizador = setInterval(cargar, 1500);
    return () => clearInterval(temporizador);
  }, [hayTransitorios, cargar]);

  const nombreCliente = useMemo(() => {
    const porId = new Map(clientes.map((c) => [c.id, c.nombre]));
    return (id: string) => porId.get(id) ?? id.slice(0, 8);
  }, [clientes]);

  const desembolsar = async (p: PrestamoDTO) => {
    try {
      await api.desembolsar(p.id);
      setAviso(`Préstamo de ${nombreCliente(p.clienteId)} desembolsado: se publicó EventoPrestamoDesembolsado.`);
      cargar();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  if (error) return <Alert color="red" title="Error" withCloseButton onClose={() => setError(null)}>{error}</Alert>;
  if (!prestamos) return <Loader mt="xl" mx="auto" display="block" />;

  return (
    <Stack gap="lg">
      <Group justify="space-between" align="end">
        <Group gap="sm" align="end">
          <Title order={3}>Préstamos</Title>
          {hayTransitorios && (
            <Badge variant="light" color="yellow" className="evaluando">
              evaluación asíncrona en curso…
            </Badge>
          )}
        </Group>
        <Button onClick={() => setModalSolicitud(true)}>Nueva solicitud</Button>
      </Group>

      {aviso && (
        <Alert color="pino" withCloseButton onClose={() => setAviso(null)}>
          {aviso}
        </Alert>
      )}

      <Card padding="0">
        <Table verticalSpacing="sm" horizontalSpacing="lg" highlightOnHover>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Cliente</Table.Th>
              <Table.Th>Producto</Table.Th>
              <Table.Th ta="right">Monto</Table.Th>
              <Table.Th ta="right">Plazo</Table.Th>
              <Table.Th ta="right">Cuota est.</Table.Th>
              <Table.Th>Estado</Table.Th>
              <Table.Th />
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {prestamos.map((p) => (
              <Table.Tr key={p.id}>
                <Table.Td>
                  <Text fw={500}>{nombreCliente(p.clienteId)}</Text>
                  <Text size="xs" c="dimmed">{fecha(p.fechaSolicitud)}</Text>
                </Table.Td>
                <Table.Td><Badge variant="default">{p.tipo}</Badge></Table.Td>
                <Table.Td ta="right"><span className="dinero">{dinero(p.monto)}</span></Table.Td>
                <Table.Td ta="right">{p.plazoMeses} m</Table.Td>
                <Table.Td ta="right"><span className="dinero">{dinero(p.cuotaEstimada)}</span></Table.Td>
                <Table.Td>
                  <Tooltip label={p.descripcionEstado} withArrow>
                    <Badge
                      variant="light"
                      color={colorEstado(p.estado)}
                      className={estadoTransitorio(p.estado) ? "evaluando" : undefined}
                    >
                      {etiquetaEstado(p.estado)}
                    </Badge>
                  </Tooltip>
                </Table.Td>
                <Table.Td>
                  <Group gap="xs" justify="end" wrap="nowrap">
                    {p.estado === "Aprobado" && (
                      <Button size="compact-xs" onClick={() => desembolsar(p)}>
                        Desembolsar
                      </Button>
                    )}
                    <Button size="compact-xs" variant="default" onClick={() => setPlanDe(p)}>
                      Plan de pagos
                    </Button>
                  </Group>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Card>

      <FormularioSolicitud
        abierto={modalSolicitud}
        clientes={clientes}
        alCerrar={() => setModalSolicitud(false)}
        alCrear={() => {
          setAviso("Solicitud aceptada (202). El motor de scoring la está evaluando en segundo plano…");
          cargar();
        }}
      />
      <ModalPlanPagos prestamo={planDe} alCerrar={() => setPlanDe(null)} />
    </Stack>
  );
}
