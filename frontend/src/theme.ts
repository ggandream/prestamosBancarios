import { createTheme, type MantineColorsTuple } from "@mantine/core";

/** Verde pino institucional: color primario de toda la aplicación. */
const pino: MantineColorsTuple = [
  "#eef6f0",
  "#dcebe0",
  "#b6d7c1",
  "#8dc2a0",
  "#6ab184",
  "#53a672",
  "#47a169",
  "#388d58",
  "#2e7d4d",
  "#1f6c40",
];

export const theme = createTheme({
  primaryColor: "pino",
  colors: { pino },
  fontFamily:
    "'Archivo', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
  fontFamilyMonospace: "'Spline Sans Mono', ui-monospace, monospace",
  headings: {
    fontFamily: "'Fraunces', Georgia, serif",
    fontWeight: "600",
  },
  defaultRadius: "md",
  components: {
    Card: {
      defaultProps: { withBorder: true, radius: "lg" },
      styles: { root: { borderColor: "#e4ddd0" } },
    },
    Badge: {
      defaultProps: { radius: "sm" },
    },
  },
});
