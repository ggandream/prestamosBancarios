# Frontend de demostración — Préstamos UMG

SPA mínima (Vite + React + Mantine) para probar los flujos del backend de forma
ilustrativa. Es secundaria: toda la lógica vive en la API (ver README raíz).

## Requisitos

- Node 20+
- Backend corriendo en `http://localhost:8080` (por ejemplo `docker compose up -d db app`
  desde la raíz del repositorio; el perfil `dev` siembra datos de demostración).

## Ejecutar

```bash
npm install
npm run dev
```

Abre <http://localhost:5174>. El dev server proxya `/api` hacia el backend
(`vite.config.ts`), por lo que no hay problemas de CORS.

## Qué se puede demostrar

- **Préstamos**: crear una solicitud (el backend responde 202) y ver en vivo cómo
  el badge pasa de `Borrador` → `En evaluación` → `Aprobado`/`Rechazado` gracias a
  la evaluación asíncrona; desembolsar préstamos aprobados (publica el evento de
  desembolso) y consultar el plan de amortización con método francés o alemán.
- **Clientes**: registrar clientes individuales o empresariales y ver su capacidad
  de pago calculada por el dominio.
- **Cartera**: el reporte con totales, índice de mora, distribución por riesgo y
  las conclusiones generadas por el análisis de cartera.

## Scripts

- `npm run dev` — servidor de desarrollo con proxy al backend.
- `npm run build` — build de producción (`dist/`).
- `npm run typecheck` — verificación de tipos.
