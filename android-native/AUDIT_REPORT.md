# Auditoría técnica empresarial CuidaVoz

## 1. Resumen ejecutivo

- Estado general: la app nativa compila, pasa unit tests y genera APK debug/release, pero no está lista para publicación real.
- Decisión: no listo para publicar. A lo sumo listo para demo interna controlada. Beta cerrada solo después de cerrar los bloqueantes de Firebase y del seed demo.
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

- Módulos Gradle activos: solo `:app` en [settings.gradle.kts](/home/gerson/cursor/cuida-voz/android-native/settings.gradle.kts:1).
- Package name: `com.cuidavoz.mobile` en [app/build.gradle.kts](/home/gerson/cursor/cuida-voz/android-native/app/build.gradle.kts:10).
- SDKs: `minSdk 26`, `targetSdk 35`, `compileSdk 35` en [app/build.gradle.kts](/home/gerson/cursor/cuida-voz/android-native/app/build.gradle.kts:11).
- Dependencias principales: Compose, Room, DataStore, Coil, Firebase Auth/Firestore/Messaging/Storage.
- Arquitectura actual: capas `data/`, `domain/`, `ui/`, `reminders/`, `voice/`.
- Punto de entrada: [CuidaVozApp.kt](/home/gerson/cursor/cuida-voz/android-native/app/src/main/java/com/cuidavoz/mobile/CuidaVozApp.kt:11) y [MainActivity.kt](/home/gerson/cursor/cuida-voz/android-native/app/src/main/java/com/cuidavoz/mobile/MainActivity.kt:14).
- Background components: `MedicationAlarmReceiver`, `MedicationBootReceiver`, `MedicationReminderVoiceService`, `MedicationNotificationActionReceiver` en [AndroidManifest.xml](/home/gerson/cursor/cuida-voz/android-native/app/src/main/AndroidManifest.xml:53).
- Room local: [CuidaVozDatabase.kt](/home/gerson/cursor/cuida-voz/android-native/app/src/main/java/com/cuidavoz/mobile/data/local/CuidaVozDatabase.kt:20).
- Firebase integrado: Auth anónimo, Firestore, FCM, Storage en [app/build.gradle.kts](/home/gerson/cursor/cuida-voz/android-native/app/build.gradle.kts:77) y [FirebaseSyncManager.kt](/home/gerson/cursor/cuida-voz/android-native/app/src/main/java/com/cuidavoz/mobile/data/sync/FirebaseSyncManager.kt:47).
- Archivos sensibles: `app/google-services.json`, `FIREBASE_RULES.md`, `app/src/main/res/xml/file_paths.xml`.
- Carpetas legacy/relevantes fuera de `android-native/`: `../apps/mobile/` contiene otra app móvil Expo/React Native con `android/`, `modules/` y `plugins/`; el root `../README.md` documenta además un backend Next.js.

## 3. Hallazgos críticos

| ID | Severidad | Área | Archivo | Descripción | Estado |
| --- | --- | --- | --- | --- | --- |
| CV-AUD-001 | BLOQUEANTE | Firebase | `FIREBASE_RULES.md`, ausencia de `firestore.rules`/`firebase.json` | No existe un archivo de reglas desplegable ni pruebas de reglas. Solo hay una propuesta en Markdown. | Pendiente |
| CV-AUD-002 | BLOQUEANTE | Producto / Datos | `CuidaVozApp.kt`, `CuidaVozAppContainer.kt` | La app siembra paciente, contacto y medicamentos demo automáticamente en primer inicio. | Requiere decisión |
| CV-AUD-003 | BLOQUEANTE | Room | `CuidaVozDatabase.kt` | Room destruía datos ante migraciones faltantes con `fallbackToDestructiveMigration()`. | Corregido |

## 4. Hallazgos altos/medios/bajos

### CV-AUD-001

- Severidad: `BLOQUEANTE`
- Área: `Firebase / Seguridad`
- Descripción: no hay evidencia de reglas Firestore reales en el repo ni de tests con Emulator. Solo existe documentación propuesta.
- Evidencia: [FIREBASE_RULES.md](/home/gerson/cursor/cuida-voz/android-native/FIREBASE_RULES.md:1) describe reglas sugeridas; la búsqueda del repo no encontró `firestore.rules`, `firebase.json` ni `.firebaserc`.
- Archivo(s): `FIREBASE_RULES.md`
- Riesgo: si el proyecto Firebase productivo está abierto o desalineado, se pueden leer o escribir datos médicos de otras familias.
- Corrección recomendada: crear `firestore.rules` y `firebase.json`, desplegar reglas mínimas por familia/rol y agregar tests con Emulator.
- Estado: `pendiente`

### CV-AUD-002

- Severidad: `BLOQUEANTE`
- Área: `Producto / UX / Sync`
- Descripción: el arranque de la app siempre llama `ensureBaselineData()` y puede poblar datos demo reales.
- Evidencia: [CuidaVozApp.kt](/home/gerson/cursor/cuida-voz/android-native/app/src/main/java/com/cuidavoz/mobile/CuidaVozApp.kt:21) y [CuidaVozAppContainer.kt](/home/gerson/cursor/cuida-voz/android-native/app/src/main/java/com/cuidavoz/mobile/CuidaVozAppContainer.kt:145) crean `María Rojas`, `Juan Rojas` y varios medicamentos.
- Archivo(s): `CuidaVozApp.kt`, `CuidaVozAppContainer.kt`
- Riesgo: usuario real inicia con datos falsos, alarmas falsas y posible sync de información demo a Firebase.
- Corrección recomendada: mover el seed a un modo demo explícito o a una tarea de desarrollo; producción debe iniciar vacía o con onboarding.
- Estado: `requiere decisión`

### CV-AUD-003

- Severidad: `BLOQUEANTE`
- Área: `Room / Persistencia`
- Descripción: la base local permitía borrar toda la data si faltaba una migración.
- Evidencia: antes de esta auditoría, [CuidaVozDatabase.kt](/home/gerson/cursor/cuida-voz/android-native/app/src/main/java/com/cuidavoz/mobile/data/local/CuidaVozDatabase.kt:48) usaba `fallbackToDestructiveMigration()`.
- Archivo(s): `CuidaVozDatabase.kt`
- Riesgo: pérdida total de historial, presión, logs y recordatorios al actualizar.
- Corrección recomendada: quitar el fallback destructivo y fallar explícitamente si falta migración.
- Estado: `corregido`

### CV-AUD-004

- Severidad: `ALTO`
- Área: `Release / Hardening`
- Descripción: el tipo `release` sigue con `isMinifyEnabled = false`.
- Evidencia: [app/build.gradle.kts](/home/gerson/cursor/cuida-voz/android-native/app/build.gradle.kts:26).
- Archivo(s): `app/build.gradle.kts`
- Riesgo: mayor superficie de ingeniería inversa, strings visibles y binario menos endurecido.
- Corrección recomendada: activar R8/minify para beta cerrada y validar reglas ProGuard.
- Estado: `pendiente`

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
- Descripción: el modelo actual depende de auth anónimo y `linkCodes`; la propuesta de reglas permite `read/update` de cualquier `linkCode` a cualquier usuario autenticado.
- Evidencia: auth anónimo en [FirebaseAuthRepository.kt](/home/gerson/cursor/cuida-voz/android-native/app/src/main/java/com/cuidavoz/mobile/data/firebase/FirebaseAuthRepository.kt:22); creación/uso de `linkCodes` en [FirebaseSyncManager.kt](/home/gerson/cursor/cuida-voz/android-native/app/src/main/java/com/cuidavoz/mobile/data/sync/FirebaseSyncManager.kt:257); propuesta amplia en [FIREBASE_RULES.md](/home/gerson/cursor/cuida-voz/android-native/FIREBASE_RULES.md:114).
- Archivo(s): `FirebaseAuthRepository.kt`, `FirebaseSyncManager.kt`, `FIREBASE_RULES.md`
- Riesgo: enumeración o abuso de códigos de vinculación si las reglas reales se parecen a la propuesta.
- Corrección recomendada: limitar lectura/escritura del código, consumirlo transaccionalmente y mover validación sensible a backend/Functions.
- Estado: `pendiente`

### CV-AUD-007

- Severidad: `MEDIO`
- Área: `Room / Mantenibilidad`
- Descripción: Room no exporta schema.
- Evidencia: [CuidaVozDatabase.kt](/home/gerson/cursor/cuida-voz/android-native/app/src/main/java/com/cuidavoz/mobile/data/local/CuidaVozDatabase.kt:31) usa `exportSchema = false`.
- Archivo(s): `CuidaVozDatabase.kt`
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
- Evidencia: [settings.gradle.kts](/home/gerson/cursor/cuida-voz/android-native/settings.gradle.kts:16) solo incluye `:app`, pero `../apps/mobile/README.md` documenta otra app móvil y `../README.md` documenta además backend web.
- Archivo(s): `settings.gradle.kts`, `../apps/mobile/README.md`, `../README.md`
- Riesgo: builds erróneos, publicación del artefacto incorrecto y deuda de mantenimiento.
- Corrección recomendada: marcar oficialmente qué app es la vigente y congelar o archivar la legacy.
- Estado: `pendiente`

### CV-AUD-010

- Severidad: `MEDIO`
- Área: `UX adulto mayor`
- Descripción: el botón `Familiar / Ajustes` sigue visible en la home del paciente.
- Evidencia: [PatientHomeScreen.kt](/home/gerson/cursor/cuida-voz/android-native/app/src/main/java/com/cuidavoz/mobile/ui/screens/PatientHomeScreen.kt:166).
- Archivo(s): `PatientHomeScreen.kt`
- Riesgo: pacientes con baja alfabetización pueden entrar a zonas complejas por error.
- Corrección recomendada: bajar prominencia, exigir confirmación más fuerte o ocultarlo en modo paciente extremo.
- Estado: `requiere decisión`

### CV-AUD-011

- Severidad: `MEDIO`
- Área: `Recordatorios / Background`
- Descripción: la arquitectura de alarmas es correcta en código, pero la confiabilidad final sigue sin validación en dispositivos reales con Doze/MIUI.
- Evidencia: `AlarmManager`, `BootReceiver`, `ForegroundService` y `POST_NOTIFICATIONS/SCHEDULE_EXACT_ALARM` están presentes en [AndroidManifest.xml](/home/gerson/cursor/cuida-voz/android-native/app/src/main/AndroidManifest.xml:4) y en `reminders/`.
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

- Reglas Firebase no demostradas ni testeadas.
- Seed demo puede crear datos falsos y disparar alarmas reales.
- Release sin minify/R8.
- Recordatorios hablados y exact alarms aún requieren validación real por fabricante.
- Repo mezclado con app Expo y backend puede inducir errores operativos.

## 6. Correcciones aplicadas

- Se eliminó `fallbackToDestructiveMigration()` de Room.
- Se sanitizaron logs que exponían voz y referencias directas a medicamentos/imágenes.
- Se agregaron `data_extraction_rules.xml` y `backup_rules.xml` y se enlazaron en manifest para Android 12+.
- Se retiraron checks obsoletos de SDK en recordatorios/canal de notificación.
- Se deshabilitó el detector de lint `WrongNavigateRouteType` porque estaba crasheando el pipeline.

## 7. Correcciones pendientes

- Implementar reglas Firebase reales y tests con Emulator.
- Eliminar o aislar completamente el seed demo en builds reales.
- Activar minify/R8 para release.
- Exportar Room schema.
- Resolver los 27 warnings de lint restantes.
- Validar recordatorios hablados en dispositivos reales y fabricantes problemáticos.

## 8. Código muerto / innecesario detectado

- `../apps/mobile/` sigue activo como app legacy paralela y no forma parte del módulo Gradle nativo.
- El root documenta backend Next.js independiente.
- Recursos no usados detectados por lint: `R.color.primary`, `R.color.background_cream`.
- No se encontraron `TODO` o `FIXME` relevantes en `app/src/main/java` ni `app/src/test`.
- Hay un worktree muy sucio con rutas antiguas borradas/renombradas; no es seguro eliminar más sin limpieza controlada del repo.

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

- Todo el checklist de [QA_ENTERPRISE_AUDIT.md](/home/gerson/cursor/cuida-voz/android-native/QA_ENTERPRISE_AUDIT.md:1)
- Firebase en dos celulares reales
- Alarmas con app cerrada y teléfono bloqueado
- Reinicio del celular
- Xiaomi/MIUI ahorro de batería
- Flujo cuidador con internet intermitente
- Backup/restore con datos reales

## 11. Recomendación final

- No publicar en Play Store todavía.
- Sí se puede usar para demo técnica controlada.
- Para beta cerrada, cerrar primero `CV-AUD-001`, `CV-AUD-002`, `CV-AUD-004` y ejecutar QA real en dispositivos.
