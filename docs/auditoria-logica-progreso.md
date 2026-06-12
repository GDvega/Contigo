# Auditoría lógica: checkpoint de progreso

Fecha del checkpoint: 2026-06-11

Este documento registra únicamente los hallazgos abordados durante la auditoría lógica. El árbol de trabajo contiene otros cambios no relacionados que no forman parte de este checkpoint.

## Estado resumido

| Hallazgo | Estado | Resumen |
|---|---|---|
| C1 | Implementado, pendiente de Firebase Emulator | Se restringió el acceso a `linkCodes` y la creación de miembros para impedir autoagregarse a familias ajenas, manteniendo el flujo legítimo mediante código válido. |
| A3 | Cerrado | Se corrigió la estructura corrupta de `VoiceIntentParserTest` y se recuperaron casos relevantes del parser. |
| A1 | Cerrado | La cancelación de alarmas usa un `PendingIntent` equivalente al utilizado al programarlas. |
| A2 | Cerrado | El asistente de voz puede desactivarse realmente y la sincronización remota incompleta ya no reactiva el valor local. |
| A4 | Cerrado como corrección mínima completa | El borrado de presión se encola, elimina el documento remoto y se propaga a otros dispositivos mediante eventos `REMOVED` verificados contra servidor. |
| M5 | Cerrado como corrección mínima | `syncPendingNow` actualiza `lastSyncAt` únicamente cuando la cola estaba vacía o todos los elementos procesados se sincronizaron correctamente. |
| M1 | Cerrado como corrección mínima centralizada | El estado diario usa una fecha reactiva y renueva sus consultas al cruzar medianoche, manteniendo consistente la fecha usada para el rango y el cálculo. |
| M2 | Corregido en código, pendiente de validación real | Las Fases 1, 2 y 3 están completadas: listeners recientes ordenados, backfill remoto paginado y subida inicial completa mediante paginación local y batches Firestore. |
| M3 | Validado manualmente con Firebase Emulator y dos dispositivos; backfill legacy pendiente | Las Fases 1, 2, 3 y 4 están completadas en código. El flujo de imágenes de medicamentos se validó manualmente con Firestore/Storage Emulator y dos dispositivos reales en ambos roles; el backfill de imágenes legacy sigue pendiente. |
| M4 | Corregido en código, pendiente de validación real | Las Fases 1, 2 mínima y 3 están completadas: la importación construye un plan exacto, encola operaciones durables post-import y aplica `skipIfRemoteNewer` antes de escribir entidades mutables remotas. |
| Room `family_contacts` | Cerrado como corrección mínima | `MIGRATION_6_7` crea de forma idempotente la tabla requerida por los schemas v7 y v8. |
| CaregiverDashboard Fases 1 y 2 | Completadas | Se reorganizó el panel del cuidador y se completó su lógica clínica y operativa reutilizando datos existentes, sin ampliar el alcance a persistencia, sincronización, navegación ni M2/M3/M4. |

## Archivos modificados por hallazgo

### C1: seguridad de vinculación familiar

- `firestore.rules`
- `android/app/src/main/java/com/cuidavoz/mobile/data/sync/FirebaseSyncManager.kt`

### A3: VoiceIntentParserTest corrupto

- `android/app/src/test/java/com/cuidavoz/mobile/domain/VoiceIntentParserTest.kt`

### A1: cancelación de alarmas

- `android/app/src/main/java/com/cuidavoz/mobile/reminders/MedicationAlarmScheduler.kt`

### A2: desactivación real del asistente de voz

- `android/app/src/main/java/com/cuidavoz/mobile/reminders/ReminderPreferencesRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/repository/SettingsRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/ui/viewmodel/SettingsViewModel.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/ui/viewmodel/VoiceAssistantViewModel.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/sync/FirebaseSyncManager.kt`

### A4: sincronización del borrado de presión

- `android/app/src/main/java/com/cuidavoz/mobile/data/repository/PressureRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/sync/FirebaseSyncManager.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/firebase/FirestorePressureRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/local/SyncQueueDao.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/local/BloodPressureDao.kt`
- `firestore.rules`

Correcciones realizadas:

- `PressureRepository` encola una operación `DELETE` antes de borrar la lectura local.
- `FirebaseSyncManager` procesa `SyncOperation.DELETE` para `PRESSURE_READING`.
- `FirestorePressureRepository` elimina el documento remoto usando la misma ruta utilizada al crearlo.
- `firestore.rules` permite borrar lecturas únicamente a pacientes o cuidadores autorizados de la familia.
- El listener de presión procesa `ADDED`, `MODIFIED` y `REMOVED`.
- Antes de borrar localmente por un evento `REMOVED`, el listener verifica contra `Source.SERVER` que el documento realmente ya no exista, evitando falsos borrados causados por `limit(50)`.
- El listener no reinserta lecturas con un `DELETE` pendiente y no elimina lecturas con un `CREATE` pendiente.

### M5: `lastSyncAt` ante fallos de sincronización

- `android/app/src/main/java/com/cuidavoz/mobile/data/sync/FirebaseSyncManager.kt`

Correcciones realizadas:

- `syncPendingNow` registra si uno o más elementos fallaron durante el procesamiento.
- Los elementos fallidos permanecen como `FAILED` mediante `markFailed` y no detienen el procesamiento de los siguientes elementos.
- `lastSyncAt` solo se actualiza cuando no hubo fallos; una cola vacía continúa contando como sincronización exitosa.

### M1: estado diario después de medianoche

- `android/app/src/main/java/com/cuidavoz/mobile/data/repository/DailyStatusRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/util/DateTimeUtils.kt`
- `android/app/src/test/java/com/cuidavoz/mobile/util/DateTimeUtilsTest.kt`

Correcciones realizadas:

- El rango del día ya no queda capturado una sola vez al crear el flujo de estado diario.
- `DailyStatusRepository` depende de una fecha reactiva y renueva las consultas mediante `flatMapLatest` después de medianoche.
- `startOfDay`, `endOfDay` y `DailyStatusCalculator` usan la misma fecha emitida, evitando estados mezclados entre días.
- `DateTimeUtils` permite calcular el rango de una fecha explícita sin romper los consumidores existentes.

### M2: sincronización inicial y listeners limitados

Estado: corregido en código; Fases 1, 2 y 3 completadas. Pendiente de validación con Firebase Emulator y prueba multidispositivo.

- `android/app/src/main/java/com/cuidavoz/mobile/data/sync/FirebaseSyncManager.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/firebase/FirestorePressureRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/firebase/FirestoreMedicationLogRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/firebase/FirestorePage.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/local/BloodPressureDao.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/local/MedicationLogDao.kt`

Correcciones realizadas en Fase 1:

- El listener de `pressureReadings` usa `measuredAt DESCENDING` y mantiene `limit(50)`.
- El listener de `medicationLogs` usa `scheduledAt DESCENDING` y mantiene `limit(200)`.
- Los listeners ya no dependen del orden arbitrario por ID de documento y conservan la lógica existente de inserción, actualización y borrado.

Correcciones realizadas en Fase 2:

- Antes de adjuntar los listeners recientes se ejecuta un backfill histórico paginado para presión y logs de medicamentos.
- Las páginas de presión se ordenan por `measuredAt DESCENDING` y las de logs por `scheduledAt DESCENDING`.
- La paginación usa el último `DocumentSnapshot` como cursor mediante `startAfter`.
- Las páginas se insertan en Room de forma idempotente usando las claves primarias remotas y `OnConflictStrategy.REPLACE`.
- El backfill de presión respeta operaciones `DELETE` pendientes y evita reinsertar esas lecturas.

Correcciones realizadas en Fase 3:

- Archivos modificados: `FirebaseSyncManager.kt`, `BloodPressureDao.kt`, `MedicationLogDao.kt`, `FirestorePressureRepository.kt` y `FirestoreMedicationLogRepository.kt`.
- `pushLocalSnapshot` ya no limita la subida inicial mediante `take(20)` para presión ni `take(50)` para logs.
- El histórico local completo se recorre mediante paginación keyset con páginas de 200 registros, sin usar `OFFSET`.
- Las lecturas de presión se ordenan por `measuredAt DESC, id DESC` y usan ambos campos como cursor estable.
- Los logs de medicamentos se ordenan por `scheduledFor DESC, id DESC` y usan ambos campos como cursor estable.
- Cada página se sube mediante batches Firestore con `batch.set()`.
- Los documentos conservan sus IDs locales y rutas existentes, manteniendo la idempotencia en reintentos.
- `createLinkCode` continúa esperando a que finalice la subida inicial y no crea el código si falla una página.

### M3: sincronización de imágenes de medicamentos

Estado: Fases 1, 2, 3 y 4 completadas en código. Validado manualmente con Firebase Emulator y dos dispositivos reales (2026-06-10). Pendiente: backfill de imágenes legacy.

- `android/app/src/main/java/com/cuidavoz/mobile/domain/sync/MedicationImageSyncOperation.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/ui/viewmodel/MedicationsViewModel.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/repository/MedicationRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/sync/FirebaseSyncManager.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/firebase/FirebaseStorageRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/firebase/FirestoreMedicationRepository.kt`
- `storage.rules`
- `firebase.json`
- `docs/FIREBASE_RULES.md`

Correcciones realizadas en Fase 1:

- El listener remoto obtiene el medicamento local antes de construir e insertar la versión recibida desde Firestore.
- Al aplicar medicamentos remotos se conserva `local?.imageUri`, evitando reemplazar una imagen local válida con `null`.
- Se protege el eco remoto con el mismo `updatedAt`.
- Se protegen actualizaciones remotas posteriores sin información de imagen.
- Los documentos legacy con `imageUrl = null` no se interpretan como una eliminación de imagen.
- El resto de campos remotos continúa aplicándose con el comportamiento existente y mediante acceso directo al DAO.

Correcciones realizadas en Fase 2:

- Se agregó la operación explícita de sincronización de imagen `KEEP`, `UPLOAD` y `DELETE`.
- `UPLOAD` sube la imagen a Firebase Storage antes de escribir el medicamento en Firestore.
- Las imágenes usan el path estable `families/{familyId}/patients/{patientId}/medications/{medicationId}.jpg`.
- Firestore guarda `imagePath` únicamente cuando corresponde; ya no se escribe `imageUrl = null` incondicionalmente.
- `KEEP` omite `imagePath` del merge de Firestore, conservando el valor remoto existente.
- Como ajuste posterior de semántica, desactivar un medicamento ya no implica `DELETE` de imagen.
- La desactivación usa `softDelete`, que conserva `imageUri` local, y sincroniza con `KEEP`, que conserva `imagePath` remoto.
- La acción explícita `Quitar imagen` continúa usando `DELETE`.
- Cambiar o elegir una imagen usa `UPLOAD`.

Correcciones realizadas en Fase 3:

- Archivos modificados: `FirebaseSyncManager.kt`, `FirebaseStorageRepository.kt` y `MedicationImageStorage.kt`.
- El listener remoto descarga imágenes desde Firebase Storage usando el `imagePath` recibido desde Firestore.
- Un `imagePath` ausente se trata como documento legacy y conserva el `imageUri` local.
- Un `imagePath` String no vacío descarga la imagen y guarda en Room la nueva URI local administrada.
- Un `imagePath = null` explícito elimina la referencia local y borra únicamente el archivo local administrado.
- La descarga se realiza primero a un archivo temporal y solo se confirma en el almacenamiento local después de completarse correctamente.
- Si la descarga falla, se conserva la imagen local anterior.
- El listener usa `MedicationDao` directamente, no llama a `MedicationRepository` ni encola operaciones, evitando loops de sincronización.
- Descargar una imagen no modifica `updatedAt` ni provoca una nueva subida.

Correcciones realizadas en Fase 4:

- Se creó `storage.rules` para el path `families/{familyId}/patients/{patientId}/medications/{fileName}`.
- La lectura, creación, actualización y eliminación requieren que el usuario autenticado pertenezca a la familia.
- La membresía familiar se valida mediante `firestore.exists()` sobre el documento `families/{familyId}/members/{uid}`.
- Las operaciones de creación y actualización aceptan únicamente archivos con `contentType` compatible con `image/*` y tamaño máximo de 5 MB.
- Cualquier otro path de Storage queda denegado.
- `firebase.json` registra `storage.rules`.
- `docs/FIREBASE_RULES.md` documenta el path, restricciones, validación de membresía y necesidad de probar con Firebase Emulator.

Validación manual con Firebase Emulator y dos dispositivos reales (2026-06-10):

- La prueba se realizó con dos dispositivos físicos reales: Xiaomi y Motorola.
- Los roles de paciente y cuidador se intercalaron ocasionalmente entre ambos dispositivos.
- Esto refuerza que la sincronización M3 validada no dependió de un único modelo de dispositivo ni de un rol fijo por teléfono.
- Firestore Emulator validado.
- Storage Emulator validado.
- Subida de imagen validada desde paciente y cuidador.
- Campo `imagePath` en Firestore validado.
- Descarga y visualización remota de imagen validadas en ambos sentidos (paciente ↔ cuidador).
- Medicamentos sin imagen sincronizan correctamente en ambos sentidos.
- Medicamentos con imagen sincronizan correctamente en ambos sentidos.
- El incidente restante se resolvió reinstalando la APK debug actual y limpiando estado local.

### M4: resincronización después de importar backup

Estado: corregido en código; Fase 1, Fase 2 mínima y Fase 3 completadas. Pendiente de validación Gradle real, Firebase Emulator y prueba multidispositivo.

- `android/app/src/main/java/com/cuidavoz/mobile/data/backup/BackupRestoreSyncPlan.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/backup/BackupRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/sync/FirebaseSyncManager.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/local/SyncQueueDao.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/di/AppModule.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/firebase/FirestoreMedicationRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/firebase/FirestorePatientRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/firebase/FirestoreHealthSettingsRepository.kt`

Correcciones realizadas en Fase 1:

- `BackupRestoreSyncPlan` registra las entidades realmente aplicadas después de importar.
- En `MERGE`, el plan excluye duplicados y entidades descartadas porque el estado local tiene un `updatedAt` más reciente.
- En `REPLACE_ALL`, el plan incluye únicamente las entidades restauradas correctamente y no representa eliminaciones remotas de entidades ausentes.
- Las imágenes restauradas correctamente se clasifican como `UPLOAD`.
- Las imágenes faltantes, fallidas o sin metadatos confiables se clasifican como `KEEP`; la ausencia de imagen nunca genera `DELETE`.
- Las preferencias restauradas no se incluyen en el plan porque no tienen un `updatedAt` fiable.

Correcciones realizadas en Fase 2 mínima:

- `FirebaseSyncManager.enqueueBackupRestore(plan)` convierte el plan exacto en operaciones durables de `SyncQueue`.
- El flujo post-import no usa `pushLocalSnapshot` ni llama a los métodos `enqueue*` públicos que disparan sincronización individual.
- Todas las operaciones se insertan primero como lote y `syncPendingNow` se ejecuta una sola vez después del encolado.
- Si Firebase no está disponible o la sincronización está desactivada, las operaciones permanecen `PENDING`.
- Antes de insertar, se eliminan únicamente operaciones equivalentes `PENDING` o `FAILED` con el mismo tipo, ID y operación.
- Las operaciones `SYNCING` no se eliminan y no se agregaron índices únicos ni migraciones Room.
- Las entidades mutables incluyen `skipIfRemoteNewer = true` en el payload; su aplicación efectiva queda registrada en Fase 3.

Correcciones realizadas en Fase 3:

- Archivos modificados: `FirebaseSyncManager.kt`, `FirestoreMedicationRepository.kt`, `FirestorePatientRepository.kt` y `FirestoreHealthSettingsRepository.kt`.
- `syncPendingNow` aplica `skipIfRemoteNewer` cuando viene en el payload de restauración.
- La protección se aplica a `PATIENT`, `MEDICATION`, `HEALTH_SETTINGS` y `FAMILY_CONTACT`.
- La protección no se aplica a `PRESSURE_READING`, `MEDICATION_LOG` ni `REMINDER_PREFERENCES`.
- Antes de escribir entidades protegidas se lee el documento remoto con `Source.SERVER`.
- La comparación usa `remoteUpdatedAt > restoredUpdatedAt`.
- Si el remoto es más reciente, se omite la escritura remota y la operación queda sincronizada/omitida exitosamente para evitar reintentos infinitos.
- Si falla la lectura remota por red o permisos, no se escribe y la operación queda `FAILED` para reintento.
- En `MEDICATION`, la comparación ocurre antes de `UPLOAD` o `DELETE` de imagen, por lo que no se sube ni se borra Storage si el medicamento remoto es más reciente.
- No se cambió el `updatedAt` restaurado por un timestamp actual.

### Migración Room de `family_contacts`

Estado: cerrado como corrección mínima de migración Room.

- Archivo modificado: `android/app/src/main/java/com/cuidavoz/mobile/data/local/ContigoDatabase.kt`.
- Motivo: los schemas exportados v7 y v8 exigen la tabla `family_contacts`, pero `MIGRATION_6_7` no la creaba explícitamente.
- Solución: se agregó `CREATE TABLE IF NOT EXISTS family_contacts` a `MIGRATION_6_7` usando la definición exacta del schema actual.
- No se modificaron entidades, DAOs, repositorios ni ViewModels.
- No se usó `fallbackToDestructiveMigration`.
- No se agregó test de migración instrumentado porque falta la dependencia `room-testing` y no existe un schema exportado v6 que permita reproducir esa base antigua.

### Mejora visual Fase 1 del CaregiverDashboard

Estado: completada el 2026-06-10.

Archivos modificados:

- `android/app/src/main/java/com/cuidavoz/mobile/ui/screens/caregiver/CaregiverDashboardScreen.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/ui/viewmodel/CaregiverDashboardViewModel.kt`

Nuevo layout:

- Header compacto con nombre del paciente y acciones existentes para volver y llamar.
- Card `Estado de hoy` con información directa, sin porcentaje ni barra de progreso.
- Card de pendiente principal o próxima toma con nombre, dosis, horario, estado e imagen disponible.
- Secciones separadas para medicamentos pendientes y medicamentos ya tomados.
- Card de última presión con valores, pulso disponible, clasificación y fecha/hora.
- Sección de actividad reciente reutilizando los textos ya calculados por el ViewModel.
- Estado de sincronización compacto debajo del resumen clínico, resaltado cuando requiere atención.
- Acciones administrativas conservadas y movidas al final del panel.

Alcance confirmado:

- No se modificaron Room, entidades ni DAOs.
- No se modificaron `FirebaseSyncManager`, `SyncQueue` ni reglas Firebase.
- No se modificaron navegación, recordatorios ni backup/importación.
- No se modificaron M2, M3 ni M4.
- Se mantuvieron los campos compartidos por `LinkCaregiverScreen` para conservar compatibilidad.

Validación de la Fase 1:

- `git diff --check`: limpio.
- `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew testDebugUnitTest`: `BUILD SUCCESSFUL`.
- `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:compileDebugKotlin`: `BUILD SUCCESSFUL`.
- `installDebug`: pendiente; al finalizar la implementación no había dispositivos conectados según `adb devices`.

### Mejora lógica Fase 2 del CaregiverDashboard

Estado: completada el 2026-06-11.

Archivos modificados:

- `android/app/src/main/java/com/cuidavoz/mobile/ui/viewmodel/CaregiverDashboardViewModel.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/ui/screens/caregiver/CaregiverDashboardScreen.kt`

Cambios implementados:

- La actividad reciente combina tomas, medicamentos omitidos y mediciones de presión.
- La actividad reciente muestra como máximo cinco eventos ordenados por fecha descendente.
- Los medicamentos tienen estados explícitos para atrasados, pendientes ahora, próximos y tomados.
- La última presión mostrada corresponde a la última medición histórica confirmada.
- Las mediciones de presión alta o crítica generan una alerta.
- Las mediciones de más de 24 horas se marcan como `Última medición antigua`.
- El estado de sync se normalizó como `Sincronización activa`, `Sin conexión`, `Sincronización pausada`, `Hay cambios pendientes` y `Requiere atención`.
- Se conservaron las acciones existentes de vinculación, activación y sincronización manual.

Datos y componentes reutilizados:

- `MedicationLogRepository`.
- `PressureRepository`.
- Estados `TAKEN` y `SKIPPED`.
- Clasificaciones de presión existentes.
- `FirebaseSyncManager.syncStatusText`.
- `AppCard`, `AppButton` y `MedicationImagePreview`.

Pendientes:

- No se muestra la cantidad exacta ni el detalle de errores de `SyncQueue` porque esa información no está expuesta sin modificar DAO o sincronización.
- No se agregaron eventos de ayuda ni eventos internos de sync.
- Siguen recomendadas las pruebas manuales visuales.

Alcance confirmado:

- No se modificaron Room ni DAOs.
- No se modificaron `FirebaseSyncManager`, `SyncQueue` ni reglas Firebase.
- No se modificaron navegación, recordatorios ni backup.
- No se modificaron M2, M3 ni M4.

Validación de la Fase 2:

- `git diff --check`: correcto.
- `testDebugUnitTest`: exitoso.
- `compileDebugKotlin`: exitoso.
- `installDebug`: exitoso en un dispositivo.
- `connectedDebugAndroidTest`: no ejecutado.

## Validaciones ejecutadas

- `./gradlew testDebugUnitTest`: `BUILD SUCCESSFUL`.
- `./gradlew :app:compileDebugKotlin`: `BUILD SUCCESSFUL`.
- Validación acumulada local: `git diff --check` global limpio, sin errores.
- Validación acumulada local: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew testDebugUnitTest` con `BUILD SUCCESSFUL`.
- Validación acumulada local: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:compileDebugKotlin` con `BUILD SUCCESSFUL`.
- Limpieza de trailing whitespace completada sin cambios de lógica, imports ni formateo completo.
- `git diff --check -- storage.rules firebase.json docs/FIREBASE_RULES.md`: limpio.
- `git diff --check` del ajuste de semántica de desactivación de M3 Fase 2: limpio.
- `git diff --check` de M3 Fase 3: limpio después de la limpieza acumulada de whitespace.
- `git diff --check` de M4 Fases 1, 2 mínima y 3: limpio después de la limpieza acumulada de whitespace.
- `ReadLints` de M4 Fase 3: sin errores en los archivos tocados.
- `./gradlew testDebugUnitTest` de M4 Fase 3: validado dentro de la ejecución acumulada local con Java 17.
- `./gradlew :app:compileDebugKotlin` de M4 Fase 3: validado dentro de la ejecución acumulada local con Java 17.
- `git diff --check` de M4 Fase 3 y limpieza posterior de whitespace: limpio.
- `firebase.json` validado con `jq`: correcto.
- Revisión de `git diff --check` sobre `DailyStatusRepository.kt` y `DateTimeUtils.kt`: limpia.
- Revisión de `git diff --check` sobre los archivos intervenidos: limpia después de eliminar únicamente whitespace final.
- Revisión manual de reglas y flujo de vinculación C1.
- Firebase CLI instalado y autenticado mediante `firebase-tools`.
- Proyecto Firebase activo: `default (cuidavoz-b4197)`.
- `storage.rules` desplegadas correctamente.
- Firebase Emulator disponible con Java 21 para Firestore y Storage.
- Migración Room de `family_contacts`: `git diff --check` limpio.
- Migración Room de `family_contacts`: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew testDebugUnitTest` con `BUILD SUCCESSFUL`.
- Migración Room de `family_contacts`: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:compileDebugKotlin` con `BUILD SUCCESSFUL`.
- M3: validación manual con Firebase Emulator y dos dispositivos reales (2026-06-10): Firestore Emulator, Storage Emulator, subida desde paciente y cuidador, `imagePath` en Firestore, descarga/visualización remota bidireccional, medicamentos sin imagen y con imagen en ambos sentidos.
- M3: incidente restante resuelto reinstalando la APK debug actual y limpiando estado local.

## Validación instrumentada pendiente

- `connectedDebugAndroidTest` queda pospuesto para una ejecución posterior en un Android Emulator limpio y estable.
- Esta validación pendiente no bloquea el avance actual: `testDebugUnitTest`, `compileDebugKotlin` y `git diff --check` están OK.
- Entorno recomendado para retomarla: Pixel 4 o Pixel 5, API 34, x86_64, imagen Google APIs, gráficos ANGLE o Software y animaciones apagadas.

## Pruebas instrumentadas: dispositivo físico no confiable

Fecha de la observación: 2026-06-07.

Estado: **confirmado**. El dispositivo físico Xiaomi/MIUI queda **descartado** para instrumented tests (`connectedDebugAndroidTest`). Este celular se reserva **únicamente para prueba manual de la APK instalada**.

### Evidencia exacta

- `adb` detecta el dispositivo correctamente.
- `adb uninstall com.cuidavoz.mobile` falla con `DELETE_FAILED_INTERNAL_ERROR`.
- `adb uninstall com.cuidavoz.mobile.test` falla con `DELETE_FAILED_INTERNAL_ERROR`.
- `adb shell pm clear` falla.
- `connectedDebugAndroidTest` queda colgado al 98%.
- `adb shell am instrument` falla con: `Unable to find instrumentation info for com.cuidavoz.mobile.test/androidx.test.runner.AndroidJUnitRunner`.
- `logcat` está saturado de ruido del sistema/MIUI y otras apps, sin traza útil de Contigo/CuidaVoz.

### Interpretación

- Como `uninstall` y `pm clear` fallan, la APK de test/instrumentación no queda disponible y por eso el runner no se encuentra y `connectedDebugAndroidTest` no avanza más allá del 98%.
- La **APK manual sí funciona** (instalación y uso manual en el dispositivo), pero **eso no valida instrumented tests ni migraciones** Room: una instalación manual no ejercita el runner de instrumentación ni los escenarios automatizados de M2/M3/M4.

### Validaciones locales que siguen limpias

- `./gradlew testDebugUnitTest`: pasa.
- `./gradlew :app:compileDebugKotlin`: pasa.
- `git diff --check`: limpio.

### Decisión y próximo paso

- No se validará `connectedDebugAndroidTest` ni M2/M3/M4 automatizados en este celular físico.
- Para instrumented tests se usará un **Android Emulator limpio**, no el celular físico Xiaomi/MIUI.
- `connectedDebugAndroidTest` queda pospuesto hasta disponer de un emulador limpio y estable.
- Entorno recomendado: Pixel 4 o Pixel 5, API 34, x86_64, imagen Google APIs, gráficos ANGLE o Software y animaciones apagadas.

## Entorno Firebase y nota operacional

- Para Firebase CLI y Emulator Suite se usa Java 21.
- Para Gradle se usa Java 17.
- No usar `firebase emulators:exec` si ya existe una instancia de emuladores abierta ocupando los puertos 8080 o 9199.
- Opción A definida como flujo operativo: Terminal 1 con Java 21 ejecutando `firebase emulators:start --only firestore,storage`; Terminal 2 con Java 17 ejecutando Gradle.

## Riesgos pendientes

- A4 aún requiere una prueba manual multidispositivo con Firebase Emulator para confirmar la propagación completa de eliminaciones remotas.
- A futuro podría ser necesario adoptar tombstones o soft-delete para manejar historiales, auditoría y conflictos de borrado más complejos.
- M5 aún no tiene pruebas unitarias localizadas para `syncPendingNow`.
- Como mejora futura, podría agregarse `lastAttemptAt` separado de `lastSyncAt` para registrar intentos fallidos sin presentarlos como sincronizaciones exitosas.
- M1 aún no tiene una prueba del flujo completo con Room y un reloj inyectable.
- Un cambio manual de zona horaria se refleja al siguiente despertar del flujo diario.
- M2 aún requiere pruebas específicas de las Fases 2 y 3 contra Firebase Emulator.
- M2 aún requiere una prueba multidispositivo para confirmar la descarga y subida completas del histórico.
- Un histórico remoto muy grande puede retrasar la conexión de los listeners recientes mientras termina el backfill.
- Un histórico local muy grande puede hacer más lenta la creación del código de vinculación.
- Los documentos legacy sin `measuredAt` o `scheduledAt` no participan en las consultas ordenadas.
- Las imágenes locales existentes anteriores a M3 Fase 2 no se suben automáticamente hasta que sean reemplazadas o se implemente un backfill.
- Operaciones offline múltiples sobre la misma imagen pueden dejar un `UPLOAD` antiguo fallido.
- Sin hash o versión de imagen pueden producirse descargas redundantes.
- Si fallan todos los intentos acotados de descarga remota de imagen, el siguiente reintento depende de recibir un nuevo evento remoto o reiniciar los listeners.
- M3 mantiene pendiente el backfill legacy de imágenes.
- Como mejora futura de M3: desacoplar imagen y medicamento en el modelo de sincronización.
- Como mejora futura operativa: Auth Emulator para pruebas locales sin credenciales reales.
- Como mejora futura operativa: `network_security_config` dinámico en lugar de la configuración fija de debug.
- M4 no resincroniza preferencias restauradas por falta de un `updatedAt` fiable.
- M4 con `REPLACE_ALL` no elimina documentos remotos ausentes.
- La restauración local y la creación de la cola post-import no forman una única transacción atómica.
- M4 requiere pruebas específicas contra Firebase Emulator y un segundo dispositivo.
- `FirebaseSyncManager` mantiene deuda de diseño para resolver conflictos de sincronización mediante `updatedAt`.
- `connectedDebugAndroidTest` no pudo validarse en el dispositivo físico Xiaomi/MIUI por fallos de `uninstall`/`pm clear` e instrumentación ausente; queda pendiente en un Android Emulator limpio.
- La migración Room de `family_contacts` requiere un test real cuando exista un schema exportado v6 e infraestructura `room-testing`, o cuando haya un AVD limpio y estable.

## Próximos hallazgos pendientes

- B1, B2 y B3: hallazgos de severidad baja pendientes.

## Próximo paso recomendado

M3 quedó validado manualmente con Firebase Emulator y dos dispositivos reales. Pendientes reales: pruebas específicas de M2 y M4 con Emulator, `connectedDebugAndroidTest` en un AVD estable, backfill legacy de imágenes M3, y mejoras futuras (desacoplar imagen/medicamento, Auth Emulator, `network_security_config` dinámico). No avanzar a nuevos hallazgos todavía.
