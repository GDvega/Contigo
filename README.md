# CuidaVoz

## Estructura

- `web/`: aplicacion web y backend en Next.js 16, Prisma y PostgreSQL.
- `android/`: aplicacion Android nativa en Kotlin con Jetpack Compose.
- `docs/`: documentacion, auditorias, reglas Firebase y checklists QA.

## Android

```bash
cd android
./gradlew clean assembleDebug lint testDebugUnitTest
```

Archivos clave:

- `android/app/google-services.json`
- `android/settings.gradle.kts`
- `android/app/build.gradle.kts`
- `android/app/src/`

## Web

```bash
cd web
npm install
npm run dev
npm run build
```

Scripts disponibles:

- `npm run lint`
- `npm run prisma:generate`
- `npm run prisma:migrate:deploy`
- `npm run db:seed`

## Firebase

- `google-services.json`: `android/app/google-services.json`
- reglas Firestore: `docs/FIREBASE_RULES.md`
- auditoria tecnica: `docs/AUDIT_REPORT.md`

## Estado

- `apps/mobile/` y Expo fueron removidos como legado.
- La raiz ya no contiene la app web ni la app Android mezcladas.
- El detalle de esta reorganizacion queda en `docs/RESTRUCTURE_REPORT.md`.
