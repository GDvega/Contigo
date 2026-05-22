# CuidaVoz

CuidaVoz mantiene dos superficies activas en este repositorio:

- `android-native/`: app Android nativa final
- raíz del proyecto: web/backend en Next.js 16 + Prisma + PostgreSQL

## Estructura actual

- `android-native/`
- `src/`
- `public/`
- `prisma/`
- `package.json`
- `.env.example`

## Ya no se usa

- Expo
- `apps/mobile/`
- `app.json` de Expo en la raíz

## Web / Backend

Instalación:

```bash
npm install
cp .env.example .env
```

Desarrollo:

```bash
npm run dev
```

Build:

```bash
npm run build
npm run lint
```

Base de datos:

```bash
npm run prisma:generate
npm run prisma:migrate:deploy
npm run db:seed
```

## Android nativo

Comandos principales:

```bash
cd android-native
./gradlew clean assembleDebug lint testDebugUnitTest
```

Archivos clave:

- `android-native/app/google-services.json`
- `android-native/settings.gradle.kts`
- `android-native/app/build.gradle.kts`
- `android-native/app/src/`

## Firebase

Ubicaciones actuales:

- Android config: `android-native/app/google-services.json`
- Reglas/documentación: `android-native/FIREBASE_RULES.md`
- Auditoría: `android-native/AUDIT_REPORT.md`
- QA enterprise: `android-native/QA_ENTERPRISE_AUDIT.md`

Nota:

- `google-services.json` se conserva porque la app Android nativa lo utiliza.
- `FIREBASE_RULES.md` documenta reglas propuestas; no reemplaza un despliegue real de reglas en Firebase.

## Despliegue web

Variables mínimas:

```env
DATABASE_URL="postgresql://USER:PASSWORD@HOST:PORT/DATABASE"
```

Comandos típicos:

```bash
npm install
npm run build
npm run start
```
