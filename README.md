# Contigo

## Estructura

- `android/`: aplicación Android nativa en Kotlin con Jetpack Compose.
- `docs/`: documentación, auditorías, reglas Firebase y checklists QA.

## Android

### Compilar y validar (local)

```bash
cd android
./gradlew clean assembleDebug lintDebug testDebugUnitTest
```

### Tests unitarios (JVM, sin dispositivo)

```bash
cd android
./gradlew testDebugUnitTest
```

Cubre dominio, recordatorios y utilidades en `app/src/test/`.

### Tests instrumentados (emulador o dispositivo)

Requieren un emulador Android en ejecución o un teléfono con depuración USB.

```bash
# Ver dispositivos conectados
adb devices

cd android
./gradlew connectedDebugAndroidTest
```

Incluye `OnboardingFlowTest` (primer ingreso: elección paciente/cuidador). El runner usa Hilt (`HiltTestRunner`).

### CI con emulador (GitHub Actions)

En el repositorio hay un workflow de ejemplo: [`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml).

Flujo resumido:

1. JDK 17
2. Gradle cache
3. Crear y arrancar un AVD (API 34, imagen Google APIs)
4. Esperar `adb devices` con estado `device`
5. `./gradlew :app:lintDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest`

Para activarlo en GitHub: push o pull request a `main`/`master`. Los tests instrumentados pueden tardar varios minutos (arranque del emulador).

**Requisitos del runner:** `ubuntu-latest` con virtualización (habitual en repos públicos y en GitHub Team/Enterprise). Si el job falla al crear el AVD, revisa los logs de `create-avd` / `emulator`.

**Sin emulador en CI:** puedes dejar solo unitarios y lint en CI y ejecutar `connectedDebugAndroidTest` manualmente antes de publicar:

```bash
./gradlew lintDebug testDebugUnitTest
```

### Logs en release

Las trazas de depuración (`ContigoLog.d/i/w`) solo se escriben en builds `debug`. Los errores genéricos (`ContigoLog.e`) siguen visibles en Logcat para diagnóstico.

Archivos clave:

- `android/app/google-services.example.json`
- `android/settings.gradle.kts`
- `android/app/build.gradle.kts`
- `android/app/src/`

## Firebase

- Copia `android/app/google-services.example.json` a `android/app/google-services.json` para desarrollo local.
- El `google-services.json` real esta ignorado por Git y debe contener una API key Android restringida.
- reglas Firestore: `docs/FIREBASE_RULES.md`
- auditoría técnica: `docs/AUDIT_REPORT.md`

## Estado

- `apps/mobile/` y Expo fueron removidos como legado.
- La superficie web (Next.js) fue removida; el repositorio contiene solo la app Android y documentación.
- El detalle de reorganizaciones anteriores queda en `docs/RESTRUCTURE_REPORT.md`.
