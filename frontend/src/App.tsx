import "@mantine/core/styles.css";
import "./global.css";
import {
  Box,
  Container,
  Group,
  MantineProvider,
  Tabs,
  Text,
  Title,
} from "@mantine/core";
import { theme } from "./theme";
import { PanelCartera } from "./paneles/PanelCartera";
import { PanelClientes } from "./paneles/PanelClientes";
import { PanelPrestamos } from "./paneles/PanelPrestamos";

export default function App() {
  return (
    <MantineProvider theme={theme}>
      <Box
        component="header"
        style={{
          background: "linear-gradient(135deg, #143d26 0%, #1f6c40 100%)",
          borderBottom: "3px solid #0e2b1b",
        }}
        py="md"
      >
        <Container size="lg">
          <Group justify="space-between" align="baseline">
            <Group gap="sm" align="baseline">
              <Title order={2} c="white" ff="'Fraunces', serif" fw={700}>
                Préstamos
              </Title>
              <Text c="pino.2" size="sm" ff="'Spline Sans Mono', monospace">
                · plataforma de gestión y evaluación
              </Text>
            </Group>
            <Text c="pino.2" size="xs" ff="'Spline Sans Mono', monospace">
              demo académica · UMG
            </Text>
          </Group>
        </Container>
      </Box>

      <Container size="lg" py="xl">
        <Tabs defaultValue="prestamos" keepMounted={false}>
          <Tabs.List mb="lg">
            <Tabs.Tab value="prestamos">Préstamos</Tabs.Tab>
            <Tabs.Tab value="clientes">Clientes</Tabs.Tab>
            <Tabs.Tab value="cartera">Cartera</Tabs.Tab>
          </Tabs.List>

          <Tabs.Panel value="prestamos">
            <PanelPrestamos />
          </Tabs.Panel>
          <Tabs.Panel value="clientes">
            <PanelClientes />
          </Tabs.Panel>
          <Tabs.Panel value="cartera">
            <PanelCartera />
          </Tabs.Panel>
        </Tabs>
      </Container>
    </MantineProvider>
  );
}
