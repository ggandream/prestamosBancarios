import { useCallback, useEffect, useState } from "react";
import {
  Alert,
  Badge,
  Button,
  Card,
  Group,
  List,
  Loader,
  SimpleGrid,
  Stack,
  Table,
  Text,
  Title,
} from "@mantine/core";
import { api, type ResumenCarteraDTO } from "../api";
import { dinero } from "../formato";

const COLOR_RIESGO: Record<string, string> = {
  ALTO: "orange",
  MEDIO: "blue",
  BAJO: "pino",
  SIN_RIESGO: "gray",
};

const NOMBRE_PRODUCTO: Record<string, string> = {
  PrestamoPersonal: "Personal",
  PrestamoHipotecario: "Hipotecario",
  PrestamoAutomotriz: "Automotriz",
};

function Cifra({ titulo, valor, detalle }: { titulo: string; valor: string; detalle?: string }) {
  return (
    <Card padding="lg">
      <Text size="xs" tt="uppercase" fw={600} c="dimmed" lts="0.08em">
        {titulo}
      </Text>
      <Title order={2} mt={4} ff="'Fraunces', serif" fw={600}>
        {valor}
      </Title>
      {detalle && (
        <Text size="sm" c="dimmed" mt={2}>
          {detalle}
        </Text>
      )}
    </Card>
  );
}

/** Dashboard del reporte de cartera (GET /api/reportes/cartera). */
export function PanelCartera() {
  const [resumen, setResumen] = useState<ResumenCarteraDTO | null>(null);
  const [error, setError] = useState<string | null>(null);

  const cargar = useCallback(() => {
    api.reporteCartera().then(setResumen).catch((e) => setError(e.message));
  }, []);

  useEffect(cargar, [cargar]);

  if (error) return <Alert color="red" title="No se pudo cargar el reporte">{error}</Alert>;
  if (!resumen) return <Loader mt="xl" mx="auto" display="block" />;

  const mora = (resumen.indiceMora * 100).toFixed(2);

  return (
    <Stack gap="lg">
      <Group justify="space-between" align="end">
        <Title order={3}>Estado de la cartera</Title>
        <Button variant="light" size="xs" onClick={cargar}>
          Actualizar
        </Button>
      </Group>

      <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
        <Cifra titulo="Préstamos" valor={String(resumen.totalPrestamos)} />
        <Cifra titulo="Monto total" valor={dinero(resumen.montoTotal)} />
        <Cifra
          titulo="Índice de mora"
          valor={`${mora}%`}
          detalle="del capital activo"
        />
        <Cifra titulo="Monto promedio" valor={dinero(resumen.montoPromedio)} />
      </SimpleGrid>

      <SimpleGrid cols={{ base: 1, md: 2 }}>
        <Card padding="lg">
          <Text size="xs" tt="uppercase" fw={600} c="dimmed" lts="0.08em" mb="sm">
            Exposición por producto
          </Text>
          <Table verticalSpacing="xs">
            <Table.Tbody>
              {Object.entries(resumen.montoPorTipo).map(([tipo, monto]) => (
                <Table.Tr key={tipo}>
                  <Table.Td>{NOMBRE_PRODUCTO[tipo] ?? tipo}</Table.Td>
                  <Table.Td>
                    <Text size="sm" c="dimmed" span>
                      {resumen.conteoPorTipo[tipo] ?? 0} préstamo(s)
                    </Text>
                  </Table.Td>
                  <Table.Td ta="right">
                    <span className="dinero">{dinero(monto)}</span>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Card>

        <Card padding="lg">
          <Text size="xs" tt="uppercase" fw={600} c="dimmed" lts="0.08em" mb="sm">
            Distribución por riesgo
          </Text>
          <Group gap="sm">
            {Object.entries(resumen.conteoPorRiesgo).map(([riesgo, conteo]) => (
              <Badge
                key={riesgo}
                size="lg"
                variant="light"
                color={COLOR_RIESGO[riesgo] ?? "gray"}
              >
                {riesgo.replace("_", " ")}: {conteo}
              </Badge>
            ))}
          </Group>
          <Text size="xs" c="dimmed" mt="md">
            ALTO = en mora · MEDIO = desembolsado · BAJO = aprobado sin
            desembolsar · SIN RIESGO = resto
          </Text>
        </Card>
      </SimpleGrid>

      <Card padding="lg">
        <Text size="xs" tt="uppercase" fw={600} c="dimmed" lts="0.08em" mb="sm">
          Conclusiones del análisis
        </Text>
        <List spacing="xs">
          {resumen.conclusiones.map((c) => (
            <List.Item key={c}>{c}</List.Item>
          ))}
        </List>
      </Card>
    </Stack>
  );
}
