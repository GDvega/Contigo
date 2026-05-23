# CLEANUP_REPORT

## 1. Fecha

- 2026-05-23

## 2. Estado del repositorio

- `web/` contiene toda la superficie Next.js, Prisma y assets web.
- `android/` contiene toda la app Android nativa y `google-services.json`.
- `docs/` concentra auditorias, reglas Firebase y checklists QA.

## 3. Limpieza aplicada

- Se removieron caches y builds generados locales.
- Se corrigieron rutas documentales que apuntaban a la carpeta Android anterior.
- Se mantuvo `android/app/google-services.json`.
- Se mantuvieron las reglas Firebase y auditorias utiles en `docs/`.

## 4. Referencias legacy

- Las menciones a Expo y `apps/mobile/` quedan solo como contexto historico en documentacion.
- El codigo activo debe usar `android/` y `web/`.

## 5. Validacion

- Ver `docs/RESTRUCTURE_REPORT.md` para el resultado final de builds y riesgos pendientes.
