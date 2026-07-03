# CLEANUP_REPORT

## 1. Fecha

- 2026-05-23 (estructura inicial)
- 2026-06-04 (eliminación de superficie web)

## 2. Estado del repositorio

- `android/` contiene toda la app Android nativa y la plantilla `google-services.example.json`.
- `docs/` concentra auditorías, reglas Firebase y checklists QA.
- La carpeta `web/` (Next.js, Prisma, PostgreSQL) fue eliminada; el producto activo es solo móvil.

## 3. Limpieza aplicada

- Se removieron caches y builds generados locales.
- Se corrigieron rutas documentales que apuntaban a la carpeta Android anterior.
- Se reemplazo `android/app/google-services.json` versionado por `android/app/google-services.example.json`.
- Se mantuvieron las reglas Firebase y auditorías útiles en `docs/`.

## 4. Referencias legacy

- Las menciones a Expo, `apps/mobile/` y la app web quedan solo como contexto histórico en documentación.
- El código activo debe usar únicamente `android/`.

## 5. Validación

- Ver `docs/RESTRUCTURE_REPORT.md` para el resultado final de builds y riesgos pendientes.
