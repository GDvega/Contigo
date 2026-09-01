# Auditoría técnica empresarial Contigo

## 1. Resumen ejecutivo

- Estado general: la app nativa compila, pasa unit tests, genera APK debug/release y tiene reglas Firestore seguras desplegadas; falta QA real antes de publicar.
- Decisión: apta técnicamente para beta cerrada después del QA en dispositivos; todavía no lista para publicación abierta.
- Score global estimado: 6.2/10.

### Score por área

| Área | Score | Estado |
| --- | --- | --- |
| Arquitectura | 7/10 | Aceptable |
| Offline-first | 7/10 | Aceptable |
| Room | 6/10 | Riesgo medio |
| Firebase | 4/10 | Bloqueante |
| Seguridad | 5/10 | Riesgo alto |
| Privacidad | 6/10 | Riesgo alto |
| Recordatorios | 7/10 | Parcialmente confiable |
| Voz | 7/10 | Parcialmente confiable |
| UX adulto mayor | 7/10 | Aceptable con riesgos |
| Accesibilidad | 6/10 | Mejorable |
| Performance | 6/10 | Mejorable |
| Release readiness | 5/10 | No apto para publicar |

## 2. Mapa técnico

- Módulos Gradle activos: solo `:app` en [settings.gradle.kts](/home/gerson/cursor/cuida-voz/android/settings.gradle.kts:1).
- Package name: `com.cuidavoz.mobile` en [app/build.gradle.kts](/home/gerson/cursor/cuida-voz/android/app/build.gradle.kts:10).
- SDKs: `minSdk 26`, `targetSdk 35`, `compileSdk 35` en [app/build.gradle.kts](/home/gerson/cursor/cuida-voz/android/app/build.gradle.kts:11).
- Dependencias principales: Compose, Room, DataStore, Coil, Firebase Auth/Firestore/Messaging/Storage.
- Arquitectura actual: capas `data/`, `domain/`, `ui/`, `reminders/`, `voice/`.
- Punto de entrada: [ContigoApp.kt](/home/gerson/cursor/cuida-voz/android/app/src/main/java/com/cuidavoz/mobile/ContigoApp.kt:11) y [MainActivity.kt](/home/gerson/cursor/cuida-voz/android/app/src/main/java/com/cuidavoz/mobile/MainActivity.kt:14).
- Background components: `MedicationAlarmReceiver`, `MedicationBootReceiver`, `MedicationReminderVoiceService`, `MedicationNotificationActionReceiver` en [AndroidManifest.xml](/home/gerson/cursor/cuida-voz/android/app/src/main/AndroidManifest.xml:53).
- Room local: [ContigoDatabase.kt](/home/gerson/cursor/cuida-voz/android/app/src/main/java/com/cuidavoz/mobile/data/local/ContigoDatabase.kt:20).
- Firebase integrado: Auth anónimo, Firestore, FCM, Storage en [app/build.gradle.kts](/home/gerson/cursor/cuida-voz/android/app/build.gradle.kts:77) y [FirebaseSyncManager.kt](/home/gerson/cursor/cuida-voz/android/app/src/main/java/com/cuidavoz/mobile/data/sync/FirebaseSyncManager.kt:47).
- Archivos sensibles: `app/google-services.json`, `FIREBASE_RULES.md`, `app/src/main/res/xml/file_paths.xml`.
- Estado post-cleanup: la app legacy `../apps/mobile/` fue eliminada del repositorio. Se conserva únicamente `android/` (la superficie web en `web/` fue eliminada en 2026-06-04).

## 3. Hallazgos críticos

| ID | Severidad | Área | Archivo | Descripción | Estado |
| --- | --- | --- | --- | --- | --- |
| CV-AUD-001 | BLOQUEANTE | Firebase | `firestore.rules`, `firebase.json`, `tests/firebase/` | Las reglas desplegables tienen pruebas de aislamiento y vinculación con Firestore Emulator. | Corregido |
| CV-AUD-002 | BLOQUEANTE | Producto / Datos | `ContigoAppInitializer.kt`, `LegacyDemoDataCleaner.kt` | La app ya no crea datos demo y limpia los datos ficticios heredados. | Corregido |
| CV-AUD-003 | BLOQUEANTE | Room | `ContigoDatabase.kt` | Room destruía datos ante migraciones faltantes con `fallbackToDestructiveMigration()`. | Corregido |

## 4. Hallazgos altos/medios/bajos

### CV-AUD-001

- Severidad: `BLOQUEANTE`
- Área: `Firebase / Seguridad`
- Descripción: las reglas Firestore están versionadas y cuentan con pruebas ejecutables mediante Emulator.
- Evidencia: `firestore.rules`, `firebase.json` y `tests/firebase/firestore.rules.test.mjs` validan aislamiento entre familias, rechazo de escrituras externas y consumo atómico de códigos de vinculación.
- Archivo(s): `firestore.rules`, `firebase.json`, `tests/firebase/`
- Riesgo residual: ejecutar las mismas pruebas ante futuros cambios de reglas.
- Estado: `corregido`

### CV-AUD-002

- Severidad: `BLOQUEANTE`
- Área: `Producto / UX / Sync`
- Descripción: el arranque ya no crea pacientes, contactos ni medicamentos de demostración.
- Evidencia: `ContigoAppInitializer` ejecuta `LegacyDemoDataCleaner` y solo programa recordatorios después de completar onboarding con un paciente real.
- Archivo(s): `ContigoAppInitializer.kt`, `LegacyDemoDataCleaner.kt`
- Riesgo residual: ninguno conocido para instalaciones nuevas; las antiguas se limpian una sola vez.
- Estado: `corregido`

### CV-AUD-003

- Severidad: `BLOQUEANTE`
- Área: `Room / Persistencia`
- Descripción: la base local permitía borrar toda la data si faltaba una migración.
- Evidencia: antes de esta auditoría, [ContigoDatabase.kt](/home/gerson/cursor/cuida-voz/android/app/src/main/java/com/cuidavoz/mobile/data/local/ContigoDatabase.kt:48) usaba `fallbackToDestructiveMigration()`.
- Archivo(s): `ContigoDatabase.kt`
- Riesgo: pérdida total de historial, presión, logs y recordatorios al actualizar.
- Corrección recomendada: quitar el fallback destructivo y fallar explícitamente si falta migración.
- Estado: `corregido`

### CV-AUD-004

- Severidad: `ALTO`
- Área: `Release / Hardening`
- Descripción: el tipo `release` usa R8 y reglas ProGuard.
- Evidencia: `android/app/build.gradle.kts` configura `isMinifyEnabled = true` y `proguard-rules.pro`.
- Archivo(s): `app/build.gradle.kts`
- Riesgo: mayor superficie de ingeniería inversa, strings visibles y binario menos endurecido.
- Estado: `corregido`

### CV-AUD-005

- Severidad: `ALTO`
- Área: `Privacidad`
- Descripción: había logs con texto reconocido por voz y referencias directas a medicamentos/archivos locales.
- Evidencia: se observaron logs en `SpeechRecognitionManager`, `MedicationImageStorage` y `MedicationsViewModel`.
- Archivo(s): `SpeechRecognitionManager.kt`, `MedicationImageStorage.kt`, `MedicationsViewModel.kt`
- Riesgo: exposición de datos médicos o frases del paciente en logcat.
- Corrección recomendada: sanitizar logs y evitar contenido clínico o transcripciones.
- Estado: `corregido`

### CV-AUD-006

- Severidad: `ALTO`
- Área: `Firebase / Vinculación`
- Descripción: los códigos temporales no se pueden listar ni actualizar y solo crean al cuidador si se eliminan en la misma transacción.
- Evidencia: `FirebaseSyncManager.linkCaregiver()` crea la membresía y elimina el código mediante `runTransaction`; `firestore.rules` exige `!existsAfter(...)`; la prueba de Emulator rechaza crear al cuidador sin consumir el código.
- Archivo(s): `FirebaseSyncManager.kt`, `firestore.rules`, `tests/firebase/firestore.rules.test.mjs`
- Riesgo residual: auth anónimo y el código temporal siguen siendo un secreto compartido; el formato actual usa 10 caracteres generados con `SecureRandom` y vence en 10 minutos.
- Estado: `corregido`

### CV-AUD-007

- Severidad: `MEDIO`
- Área: `Room / Mantenibilidad`
- Descripción: Room no exporta schema.
- Evidencia: [ContigoDatabase.kt](/home/gerson/cursor/cuida-voz/android/app/src/main/java/com/cuidavoz/mobile/data/local/ContigoDatabase.kt:31) usa `exportSchema = false`.
- Archivo(s): `ContigoDatabase.kt`
- Riesgo: migraciones más difíciles de auditar y probar.
- Corrección recomendada: exportar schemas versionados y agregarlos al repositorio.
- Estado: `pendiente`

### CV-AUD-008

- Severidad: `MEDIO`
- Área: `QA / Tooling`
- Descripción: lint era inestable por un crash del detector `WrongNavigateRouteDetector`.
- Evidencia: ejecución de `./gradlew lint` falló en `AppNavigation.kt` con `WrongNavigateRouteType`.
- Archivo(s): `app/build.gradle.kts`
- Riesgo: pipeline no confiable y falsos bloqueos de CI.
- Corrección recomendada: desactivar ese detector hasta actualizar la dependencia o AGP.
- Estado: `corregido`

### CV-AUD-009

- Severidad: `MEDIO`
- Área: `Repo / Operación`
- Descripción: hay tres superficies de producto en el mismo repo: app nativa, app Expo/React Native y backend Next.js.
- Evidencia histórica: al momento de la auditoría existían tres superficies; el repositorio actual contiene solo la app Android nativa en `android/`.
- Archivo(s): `settings.gradle.kts`, `../README.md`
- Riesgo: builds erróneos, publicación del artefacto incorrecto y deuda de mantenimiento.
- Corrección recomendada: marcar oficialmente qué app es la vigente y congelar o archivar la legacy.
- Estado: `pendiente`

### CV-AUD-010

- Severidad: `MEDIO`
- Área: `UX adulto mayor`
- Descripción: el botón `Familiar / Ajustes` sigue visible en la home del paciente.
- Evidencia: [PatientHomeScreen.kt](/home/gerson/cursor/cuida-voz/android/app/src/main/java/com/cuidavoz/mobile/ui/screens/PatientHomeScreen.kt:166).
- Archivo(s): `PatientHomeScreen.kt`
- Riesgo: pacientes con baja alfabetización pueden entrar a zonas complejas por error.
- Corrección recomendada: bajar prominencia, exigir confirmación más fuerte o ocultarlo en modo paciente extremo.
- Estado: `requiere decisión`

### CV-AUD-011

- Severidad: `MEDIO`
- Área: `Recordatorios / Background`
- Descripción: la arquitectura de alarmas es correcta en código, pero la confiabilidad final sigue sin validación en dispositivos reales con Doze/MIUI.
- Evidencia: `AlarmManager`, `BootReceiver`, `ForegroundService` y `POST_NOTIFICATIONS/SCHEDULE_EXACT_ALARM` están presentes en [AndroidManifest.xml](/home/gerson/cursor/cuida-voz/android/app/src/main/AndroidManifest.xml:4) y en `reminders/`.
- Archivo(s): `AndroidManifest.xml`, `reminders/*`
- Riesgo: recordatorios tardíos o silenciados en fabricantes agresivos.
- Corrección recomendada: validar en Samsung, Motorola y Xiaomi reales con app cerrada, reinicio y ahorro de batería.
- Estado: `requiere prueba en dispositivo real`

### CV-AUD-012

- Severidad: `BAJO`
- Área: `UI / Recursos`
- Descripción: quedan 27 warnings de lint, incluyendo dependencias desactualizadas, icono sin monochrome, recursos sin uso y `Locale.getDefault()` en constantes.
- Evidencia: `0 errors, 27 warnings` en `app/build/reports/lint-results-debug.txt`.
- Archivo(s): `app/build.gradle.kts`, `AndroidManifest.xml`, `MedicationSchedule.kt`, `MedicationsScreen.kt`, `res/`
- Riesgo: deuda acumulada y ruido de calidad.
- Corrección recomendada: resolver warnings en lote antes de beta cerrada.
- Estado: `pendiente`

## 5. Riesgos de producción

- Las reglas Firebase están probadas y producción usa la misma versión verificada por hash.
- El seed demo fue retirado y R8 está activo en release.
- Recordatorios hablados y exact alarms aún requieren validación real por fabricante.
- Históricamente el repo mezclaba app Expo y backend; después de la limpieza ese riesgo quedó reducido.

## 6. Correcciones aplicadas

- Se eliminó `fallbackToDestructiveMigration()` de Room.
- Se sanitizaron logs que exponían voz y referencias directas a medicamentos/imágenes.
- Se agregaron `data_extraction_rules.xml` y `backup_rules.xml` y se enlazaron en manifest para Android 12+.
- Se retiraron checks obsoletos de SDK en recordatorios/canal de notificación.
- Se deshabilitó el detector de lint `WrongNavigateRouteType` porque estaba crasheando el pipeline.
- Se agregaron pruebas de reglas Firestore con Emulator para aislamiento familiar y vinculación.
- Se desplegaron las reglas Firestore y se exigió el consumo atómico de cada código de vinculación.

## 7. Correcciones pendientes

- Exportar Room schema.
- Resolver los 27 warnings de lint restantes.
- Validar recordatorios hablados en dispositivos reales y fabricantes problemáticos.

## 8. Código muerto / innecesario detectado

- La app legacy `../apps/mobile/` fue retirada durante la limpieza del repositorio.
- La documentación raíz apunta a la app Android en `android/`.
- Recursos no usados detectados por lint: `R.color.primary`, `R.color.background_cream`.
- No se encontraron `TODO` o `FIXME` relevantes en `app/src/main/java` ni `app/src/test`.
- La reestructuración movió rutas antiguas a `android/` y `docs/`; la carpeta `web/` fue eliminada posteriormente.

## 9. Pruebas ejecutadas

- `./gradlew clean assembleDebug lint testDebugUnitTest` -> `BUILD SUCCESSFUL`
- `./gradlew clean assembleRelease` -> `BUILD SUCCESSFUL`
- `./gradlew assembleRelease` -> `BUILD SUCCESSFUL`
- `./gradlew detekt` -> task no configurada
- `./gradlew ktlintCheck` -> task no configurada
- Búsquedas ejecutadas:
  - permisos/exported
  - posibles secretos
  - `fallbackToDestructiveMigration`
  - `Log.*`, `println`
  - `TODO` / `FIXME`

## 10. Pruebas manuales pendientes

- Todo el checklist de [QA_ENTERPRISE_AUDIT.md](/home/gerson/cursor/cuida-voz/docs/QA_ENTERPRISE_AUDIT.md:1)
- Firebase en dos celulares reales
- Alarmas con app cerrada y teléfono bloqueado
- Reinicio del celular
- Xiaomi/MIUI ahorro de batería
- Flujo cuidador con internet intermitente
- Backup/restore con datos reales

## 11. Recomendación final

- No publicar en Play Store todavía.
- Sí se puede iniciar una beta cerrada después del QA real en dispositivos.
- Antes de publicación abierta, completar el checklist manual de recordatorios, vinculación en dos celulares y conectividad intermitente.
