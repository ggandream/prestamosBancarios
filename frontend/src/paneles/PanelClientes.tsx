import { useCallback, useEffect, useState } from "react";
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
} from "@mantine/core";
import { api, type ClienteDTO } from "../api";
import { dinero } from "../formato";

const COLOR_HISTORIAL: Record<string, string> = {
  BUENO: "pino",
  REGULAR: "yellow",
  MALO: "red",
};

/** Alta de cliente: formulario plano con campos condicionales por tipo. */
function FormularioCliente({
  abierto,
  alCerrar,
  alRegistrar,
}: {
  abierto: boolean;
  alCerrar: () => void;
  alRegistrar: () => void;
}) {
  const [tipo, setTipo] = useState<"INDIVIDUAL" | "EMPRESARIAL">("INDIVIDUAL");
  const [nombre, setNombre] = useState("");
  const [documento, setDocumento] = useState("");
  const [email, setEmail] = useState("");
  const [historial, setHistorial] = useState<string>("BUENO");
  const [salario, setSalario] = useState<number | string>(8000);
  const [tipoEmpleo, setTipoEmpleo] = useState<string>("FORMAL");
  const [antiguedadLaboral, setAntiguedadLaboral] = useState<number | string>(1);
  const [facturacion, setFacturacion] = useState<number | string>(600000);
  const [nit, setNit] = useState("");
  const [sector, setSector] = useState<string>("COMERCIO");
  const [antiguedadNit, setAntiguedadNit] = useState<number | string>(1);
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const registrar = async () => {
    setEnviando(true);
    setError(null);
    try {
      await api.registrarCliente({
        tipo,
        nombre,
        documento,
        email,
        historial,
        ...(tipo === "INDIVIDUAL"
          ? {
              salarioMensual: Number(salario),
              tipoEmpleo,
              antiguedadLaboral: Number(antiguedadLaboral),
            }
          : {
              facturacionAnual: Number(facturacion),
              nit,
              sector,
              antiguedadNit: Number(antiguedadNit),
            }),
      });
      alRegistrar();
      alCerrar();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Modal opened={abierto} onClose={alCerrar} title="Registrar cliente" size="md">
      <Stack gap="sm">
        <SegmentedControl
          fullWidth
          value={tipo}
          onChange={(v) => setTipo(v as typeof tipo)}
          data={[
            { label: "Individual", value: "INDIVIDUAL" },
            { label: "Empresarial", value: "EMPRESARIAL" },
          ]}
        />
        <TextInput label="Nombre" value={nombre} onChange={(e) => setNombre(e.currentTarget.value)} required />
        <Group grow>
          <TextInput label="Documento" value={documento} onChange={(e) => setDocumento(e.currentTarget.value)} required />
          <TextInput label="Email" type="email" value={email} onChange={(e) => setEmail(e.currentTarget.value)} required />
        </Group>
        <Select
          label="Historial crediticio"
          description="Campo almacenado; alimenta la regla de historial del scoring"
          data={["BUENO", "REGULAR", "MALO"]}
          value={historial}
          onChange={(v) => v && setHistorial(v)}
        />

        {tipo === "INDIVIDUAL" ? (
          <Group grow>
            <NumberInput label="Salario mensual" value={salario} onChange={setSalario} min={1} thousandSeparator="," prefix="Q" />
            <Select label="Tipo de empleo" data={["FORMAL", "INDEPENDIENTE", "INFORMAL"]} value={tipoEmpleo} onChange={(v) => v && setTipoEmpleo(v)} />
            <NumberInput label="Antigüedad (años)" value={antiguedadLaboral} onChange={setAntiguedadLaboral} min={0} />
          </Group>
        ) : (
          <>
            <Group grow>
              <NumberInput label="Facturación anual" value={facturacion} onChange={setFacturacion} min={1} thousandSeparator="," prefix="Q" />
              <TextInput label="NIT" value={nit} onChange={(e) => setNit(e.currentTarget.value)} required />
            </Group>
            <Group grow>
              <Select label="Sector" data={["COMERCIO", "INDUSTRIA", "SERVICIOS", "AGRICOLA", "CONSTRUCCION"]} value={sector} onChange={(v) => v && setSector(v)} />
              <NumberInput label="Antigüedad NIT (años)" value={antiguedadNit} onChange={setAntiguedadNit} min={0} />
            </Group>
          </>
        )}

        {error && <Alert color="red">{error}</Alert>}
        <Group justify="end" mt="xs">
          <Button variant="default" onClick={alCerrar}>Cancelar</Button>
          <Button onClick={registrar} loading={enviando}>Registrar</Button>
        </Group>
      </Stack>
    </Modal>
  );
}

/** Listado y alta de clientes (POST/GET /api/clientes). */
export function PanelClientes() {
  const [clientes, setClientes] = useState<ClienteDTO[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [modalAbierto, setModalAbierto] = useState(false);

  const cargar = useCallback(() => {
    api.listarClientes().then(setClientes).catch((e) => setError(e.message));
  }, []);

  useEffect(cargar, [cargar]);

  if (error) return <Alert color="red" title="No se pudo cargar clientes">{error}</Alert>;
  if (!clientes) return <Loader mt="xl" mx="auto" display="block" />;

  return (
    <Stack gap="lg">
      <Group justify="space-between" align="end">
        <Title order={3}>Clientes</Title>
        <Button onClick={() => setModalAbierto(true)}>Registrar cliente</Button>
      </Group>

      <Card padding="0">
        <Table verticalSpacing="sm" horizontalSpacing="lg" highlightOnHover>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Nombre</Table.Th>
              <Table.Th>Tipo</Table.Th>
              <Table.Th>Documento</Table.Th>
              <Table.Th>Historial</Table.Th>
              <Table.Th ta="right">Ingreso mensual</Table.Th>
              <Table.Th ta="right">Capacidad de pago</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {clientes.map((c) => (
              <Table.Tr key={c.id}>
                <Table.Td>
                  <Text fw={500}>{c.nombre}</Text>
                  <Text size="xs" c="dimmed">{c.email}</Text>
                </Table.Td>
                <Table.Td>
                  <Badge variant="default">{c.tipo === "INDIVIDUAL" ? "Individual" : "Empresarial"}</Badge>
                </Table.Td>
                <Table.Td><span className="dinero">{c.documento}</span></Table.Td>
                <Table.Td>
                  <Badge variant="light" color={COLOR_HISTORIAL[c.historial]}>{c.historial}</Badge>
                </Table.Td>
                <Table.Td ta="right"><span className="dinero">{dinero(c.ingresoMensual)}</span></Table.Td>
                <Table.Td ta="right"><span className="dinero">{dinero(c.capacidadPago)}</span></Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Card>

      <FormularioCliente
        abierto={modalAbierto}
        alCerrar={() => setModalAbierto(false)}
        alRegistrar={cargar}
      />
    </Stack>
  );
}
