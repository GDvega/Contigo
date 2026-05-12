# CuidaVoz Mobile

Aplicación Expo React Native para pacientes mayores y familiares cuidadores.

## Architecture

La app móvil ya funciona en modo offline-first:

- SQLite local para paciente, pastillas, historial, presión y reportes.
- Notificaciones locales con `expo-notifications`.
- Voz con `expo-speech` y `expo-speech-recognition`.
- Cámara y galería para imágenes de medicamentos.
- PDF médico local con `expo-print` y `expo-sharing`.

El backend Next.js se conserva para futuro sync y para flujos web. La app móvil puede ejecutarse sin internet y sin backend.

## Run Locally

1. Instala dependencias:

```bash
cd apps/mobile
npm install
```

2. Inicia la app móvil:

```bash
npx expo start --dev-client -c
```

3. Para un development build en Android:

```bash
eas build --profile development --platform android
```

4. Para un preview APK instalable:

```bash
eas build --profile preview --platform android
```

## Optional API Mode

Solo necesitas `EXPO_PUBLIC_API_URL` si quieres probar llamadas directas al backend desde código legado o futuras sincronizaciones.

Ejemplo para dispositivo físico:

```bash
cp .env.example .env
EXPO_PUBLIC_API_URL=http://192.168.0.103:3000 npx expo start --dev-client -c --host lan
```

Ejemplo para emulador Android:

```bash
EXPO_PUBLIC_API_URL=http://10.0.2.2:3000 npx expo start --dev-client -c
```

## Validation

Pruebas mínimas recomendadas:

- Abrir la app con WiFi y datos apagados.
- Confirmar que Inicio, Pastillas, Historial, Familia y Ajustes cargan datos locales.
- Registrar presión manual.
- Confirmar una pastilla.
- Generar y compartir reporte médico PDF.
- Activar recordatorios y verificar notificaciones locales.

## Folder Structure

```text
apps/mobile/
  App.tsx
  src/
    components/
    lib/
    navigation/MainTabs.tsx
    screens/
    types/
    utils/
```
