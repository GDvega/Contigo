# CuidaVoz Mobile

Expo React Native foundation for the CuidaVoz mobile app.

## Architecture

The mobile app consumes the existing Next.js API:

- `GET /api/daily-status`
- `POST /api/blood-pressure`
- `GET /api/medications`
- `POST /api/medication-logs`
- `GET /api/blood-pressure`
- `GET /api/reports/medical-summary`

The current mobile app keeps voice, real notifications, authentication, and offline sync disabled while the core health flows are validated.

## Run Locally

1. Start the web/backend app from the repo root:

```bash
npm run dev
```

2. Install and start mobile:

```bash
cd apps/mobile
npm install
cp .env.example .env
npm run start
```

For Android emulator, use:

```bash
EXPO_PUBLIC_API_URL=http://10.0.2.2:3000 npm run android
```

For a physical device, use your computer LAN IP:

```bash
EXPO_PUBLIC_API_URL=http://192.168.0.103:3000 npm run start
```

## Android Development Build

Use a development build when you are ready to test native capabilities such as local notifications, microphone permissions, camera/gallery, and future voice features.

1. Install and log in to EAS:

```bash
npm install -g eas-cli
eas login
```

2. Configure the EAS project if it has not been linked yet:

```bash
cd apps/mobile
eas build:configure
```

3. Build an Android development APK:

```bash
eas build --profile development --platform android
```

4. Install the APK on Android:

- Open the EAS build link on the Android device and download the APK.
- Allow installation from the browser or file manager if Android asks.
- Tap the downloaded APK to install CuidaVoz.
- Start Metro for the development client:

```bash
npm run start
```

For a physical device, keep `EXPO_PUBLIC_API_URL` pointing to your computer LAN IP, for example:

```bash
EXPO_PUBLIC_API_URL=http://192.168.0.103:3000 npm run start
```

## Folder Structure

```text
apps/mobile/
  App.tsx
  src/
    components/
    lib/api.ts
    navigation/MainTabs.tsx
    screens/
    types/
    utils/
```
