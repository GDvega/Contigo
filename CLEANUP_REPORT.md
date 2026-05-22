# CLEANUP_REPORT

## 1. Fecha de limpieza

- 2026-05-22

## 2. Objetivo

- Limpiar el repositorio CuidaVoz para conservar solo:
- `android-native/` como app Android nativa final
- la web/backend en la raíz del proyecto

## 3. Carpetas conservadas

- `android-native/`
- `src/`
- `public/`
- `prisma/`
- `node_modules/` local no versionado
- `.next/` local no versionado

## 4. Carpetas eliminadas

- `apps/mobile/`
- `.idea/`
- `android-native/.idea/`
- `android-native/.gradle/`
- `android-native/app/build/`
- `android-native/build/`
- `app.json`

## 5. Archivos modificados

- `.gitignore`
- `README.md`
- `eslint.config.mjs`
- `tsconfig.json`
- `android-native/AUDIT_REPORT.md`

## 6. Referencias legacy encontradas

- `README.md` conserva una sección breve indicando que Expo y `apps/mobile/` ya no se usan.
- `android-native/AUDIT_REPORT.md` conserva referencias históricas a Expo para contexto de auditoría.
- No se detectaron dependencias activas entre `android-native/` y `apps/mobile/`.
- No se detectaron dependencias activas entre la web/backend y `apps/mobile/`.

## 7. Resultado build Android

- Comando: `cd android-native && ./gradlew clean assembleDebug lint testDebugUnitTest`
- Resultado: `BUILD SUCCESSFUL`
- Observaciones:
- warnings deprecados en iconos Compose dentro de `HelpScreen.kt` y `MeasurePressureScreen.kt`
- reporte Gradle en `android-native/build/reports/problems/problems-report.html`

## 8. Resultado build web

- Comando: `npm run lint`
- Resultado: exitoso
- Comando: `npm run build`
- Resultado: exitoso
- Observaciones:
- el primer intento dentro del sandbox falló por una restricción de Turbopack al crear un proceso auxiliar
- el build real fuera del sandbox terminó correctamente

## 9. Riesgos pendientes

- `android-native/app/google-services.json` sigue versionado; revisar si esa decisión es aceptable para el proyecto/Firebase real.
- `android-native/AUDIT_REPORT.md` sigue documentando riesgos de producto y Firebase no relacionados con la limpieza.
- `android-native/local.properties` quedó fuera del control de versiones y debe seguir así.
- `.env` no fue tocado; validar que no esté versionado antes de cualquier push final.

## 10. Comandos ejecutados

```bash
git status --short --branch
find . -maxdepth 3 -type d | sort
find . -maxdepth 3 -type f | sort
cat package.json
find . -name 'package.json' -not -path '*/node_modules/*' -print | sort
find . \( -name 'settings.gradle*' -o -name 'build.gradle*' -o -name '*.gradle.kts' \) | sort
grep -RInE 'apps/mobile|expo|Expo|@cuidavoz/mobile|mobileData|EXPO_PUBLIC_API_URL' . ...
grep -RInE 'android-native|com\\.cuidavoz\\.mobile' . ...
git checkout -b cleanup/native-and-web-only
git rm -r -- .idea android-native/.idea android-native/.gradle android-native/app/build android-native/build apps/mobile app.json
git rm --cached -- android-native/local.properties
./gradlew clean assembleDebug lint testDebugUnitTest
npm run lint
npm run build
```
