# RESTRUCTURE_REPORT

## 1. Objetivo

Reorganizar Contigo para dejar la app web en `web/`, la app Android nativa en `android/` y la documentacion en `docs/`, sin tocar logica de negocio.

## 2. Estructura anterior

- Web y backend Next.js en la raiz.
- Android nativo en `android-native/`.
- Auditorias y reglas Firebase dentro de `android-native/` y en raiz.

## 3. Estructura nueva

```text
cuida-voz/
├── android/
├── docs/
├── web/
├── AGENTS.md
├── CLAUDE.md
├── CLEANUP_REPORT.md
├── README.md
└── .gitignore
```

## 4. Carpetas movidas

- `android-native/` -> `android/`
- `src/` -> `web/src/`
- `public/` -> `web/public/`
- `prisma/` -> `web/prisma/`

## 5. Archivos movidos

- A `web/`: `package.json`, `package-lock.json`, `next.config.ts`, `tsconfig.json`, `eslint.config.mjs`, `postcss.config.mjs`, `prisma.config.ts`, `components.json`, `.env.example`
- A `docs/`: `AUDIT_REPORT.md`, `FIREBASE_RULES.md`, `QA_CHECKLIST.md`, `QA_ENTERPRISE_AUDIT.md`

## 6. Carpetas eliminadas o limpiadas

- Caches y builds locales de Android y web usados durante la validacion
- Dependencias instaladas y cliente Prisma generado localmente, removidos de la estructura final para dejar el repo limpio

## 7. Archivos actualizados

- `README.md`
- `.gitignore`
- `CLEANUP_REPORT.md`
- `docs/AUDIT_REPORT.md`
- `web/components.json`

## 8. Resultado build Android

Comando ejecutado:

```bash
cd android
./gradlew clean assembleDebug lint testDebugUnitTest
```

Resultado: `BUILD SUCCESSFUL` el 2026-05-23.

Observaciones:

- Se emitieron warnings de deprecacion de iconos Compose en `android/app/src/main/java/com/cuidavoz/mobile/ui/screens/HelpScreen.kt` y `android/app/src/main/java/com/cuidavoz/mobile/ui/screens/MeasurePressureScreen.kt`.
- Gradle genero un reporte de problemas en `android/build/reports/problems/problems-report.html` durante la validacion.

## 9. Resultado build Web

Comandos ejecutados:

```bash
cd web
npm install
npm run lint
npm run build
```

Resultado: exitoso el 2026-05-23.

Observaciones:

- `npm install` regenero Prisma Client en `web/src/app/generated/prisma`.
- `next build` completo sin errores con Next.js 16.2.5 y Prisma 7.8.0.

## 10. Referencias legacy restantes

- No quedan referencias activas a `android-native/` en codigo o configuracion.
- Las menciones a Expo y `apps/mobile/` quedan solo en documentacion historica y notas de contexto.

## 11. Riesgos pendientes

- `android/app/google-services.json` debe permanecer local/CI; el repo versiona solo `android/app/google-services.example.json`.
- `docs/AUDIT_REPORT.md` sigue documentando riesgos de producto y seguridad ajenos a esta reestructuracion.
- `android/local.properties` permanece fuera de Git y debe seguir local.

## 12. Eliminación de la app web (2026-06-04)

Se eliminó por completo la carpeta `web/` (Next.js, Prisma, PostgreSQL y APIs). El repositorio queda solo con:

```text
cuida-voz/
├── android/
├── docs/
├── firestore.rules
├── firebase.json
└── ...
```

No se modificó código bajo `android/`. La sincronización y persistencia del producto siguen en Room + Firebase dentro de la app móvil.
