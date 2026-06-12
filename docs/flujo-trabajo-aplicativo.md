# Flujo de trabajo del aplicativo CuidaVoz

Este documento describe el flujo funcional y técnico actual del aplicativo Android CuidaVoz. Se basa en el análisis de navegación, ViewModels, repositorios, Room, recordatorios, backup y sincronización Firebase realizado con CodeGraph.

## 1. Resumen general del aplicativo

CuidaVoz es una aplicación Android nativa orientada al acompañamiento diario de un paciente y su cuidador o familiar. La aplicación permite:

- Registrar y administrar medicamentos, horarios e imágenes.
- Recordar y confirmar tomas de medicamentos.
- Registrar presión arterial y consultar su historial.
- Mostrar al cuidador el estado reciente del paciente.
- Generar reportes médicos en PDF.
- Exportar e importar respaldos ZIP.
- Vincular dos dispositivos mediante un código temporal.
- Sincronizar información entre dispositivos usando Firebase.

La arquitectura sigue un enfoque local-first:

1. La interfaz escribe primero en la base local Room o en DataStore.
2. Los repositorios encolan las operaciones que deben sincronizarse.
3. `FirebaseSyncManager` procesa la cola cuando existe conexión y contexto familiar.
4. Los listeners Firebase actualizan Room en el otro dispositivo.
5. Las pantallas observan Room mediante `Flow` y `StateFlow`.

La aplicación maneja dos modos de experiencia:

- **Paciente:** interfaz simplificada para confirmar tomas, medir presión y pedir ayuda.
- **Cuidador:** panel de administración, seguimiento, vinculación, reportes, ajustes y backup.

Estos modos determinan la pantalla principal y el comportamiento de recordatorios, pero no constituyen una separación rígida de navegación: desde el modo paciente se puede abrir el área del cuidador.

## 2. Flujo funcional del paciente

### Primer ingreso

1. La aplicación inicia en onboarding.
2. El usuario selecciona `Soy paciente`.
3. Registra:
   - Nombre del paciente.
   - Edad opcional.
   - Nota médica opcional.
   - Nombre, teléfono y relación del cuidador.
   - Preferencia para activar recordatorios.
4. El aplicativo guarda paciente, contacto y rangos médicos predeterminados.
5. Activa el modo fácil, la guía por voz y los recordatorios por voz.
6. Si los recordatorios están activos, programa las alarmas de medicamentos.
7. Finaliza el onboarding y abre el inicio del paciente.

El onboarding también permite restaurar un backup existente. Después de una restauración realizada desde onboarding, el dispositivo entra como paciente.

### Inicio del paciente

La pantalla principal combina información local de:

- Paciente.
- Contacto familiar.
- Medicamentos activos.
- Logs de tomas del día.
- Última presión del día.
- Recordatorio activo.

Desde esta pantalla el paciente puede:

- Ver la próxima pastilla o grupo de pastillas pendientes.
- Ver las imágenes asociadas a los medicamentos.
- Confirmar que tomó una o varias pastillas.
- Posponer un recordatorio.
- Abrir el registro de presión arterial.
- Pedir ayuda o contactar al cuidador.
- Usar el asistente de voz.
- Abrir el área del cuidador.

### Confirmación de una toma

1. El paciente pulsa `Tomé mi pastilla` o confirma mediante recordatorio o voz.
2. Se identifica la pastilla o grupo pendiente para la hora correspondiente.
3. Se crean logs locales con estado `TAKEN`.
4. Se cancela el recordatorio pendiente del grupo.
5. Se limpia el aviso activo.
6. Los logs se encolan para sincronización.

## 3. Flujo funcional del cuidador

### Primer ingreso

1. La aplicación inicia en onboarding.
2. El usuario selecciona `Soy cuidador o familiar`.
3. Registra su nombre.
4. El dispositivo guarda el rol cuidador.
5. Desactiva el modo fácil y los recordatorios locales.
6. Cancela las alarmas de medicamentos del dispositivo.
7. Abre el panel del cuidador.

### Panel del cuidador

El panel muestra información observada desde Room:

- Nombre del paciente.
- Última presión registrada.
- Clasificación de la presión.
- Cantidad de medicamentos pendientes del día.
- Eventos recientes de tomas.
- Estado de sincronización.
- Estado de vinculación familiar.

Desde el panel, el cuidador puede:

- Vincular un paciente mediante código.
- Administrar medicamentos e imágenes.
- Revisar registros de presión y medicamentos.
- Cargar presiones históricas.
- Generar reportes médicos.
- Editar contacto familiar.
- Editar ajustes y preferencias.
- Exportar o importar backups.
- Reintentar o pausar la sincronización.
- Abrir el contacto de ayuda del paciente.
- Volver al modo paciente.

Cuando el dispositivo está vinculado como cuidador, escucha alertas remotas no vistas relacionadas con:

- Presión fuera de rango.
- Toma de medicamento no confirmada.
- Solicitud de ayuda del paciente.

## 4. Flujo de vinculación paciente/cuidador

La pantalla de vinculación contiene las acciones de creación y consumo del código. Funcionalmente, el paciente genera el código y el cuidador lo introduce en su dispositivo.

### Creación del código en el dispositivo paciente

1. El paciente abre el área del cuidador.
2. Abre `Vincular cuidador o paciente`.
3. Pulsa `Crear código`.
4. `FirebaseSyncManager` asegura una sesión Firebase anónima.
5. Crea o reutiliza un `familyId`.
6. Crea el documento de familia y registra al usuario como miembro con rol `patient`.
7. Sube el snapshot local completo:
   - Paciente.
   - Medicamentos.
   - Presiones.
   - Logs de medicamentos.
   - Ajustes.
   - Contacto familiar.
8. Genera un código aleatorio de seis dígitos.
9. Guarda el código en Firestore con familia, paciente, creador y vencimiento.
10. Actualiza el contexto local de sincronización.
11. Inicia listeners en tiempo real.

El código vence después de diez minutos. La creación no termina correctamente si falla la subida inicial completa.

### Consumo del código en el dispositivo cuidador

1. El cuidador abre la pantalla de vinculación.
2. Introduce el código de seis dígitos.
3. Firebase valida que el código exista y no haya vencido.
4. Dentro de una transacción:
   - Registra al usuario como miembro con rol `caregiver`.
   - Obtiene `familyId` y `patientId`.
   - Elimina el código para impedir reutilización.
5. Registra al cuidador principal en el documento del paciente.
6. Guarda el contexto familiar local.
7. Actualiza el token FCM.
8. Inicia listeners y backfill histórico.
9. Procesa operaciones locales pendientes.

### Diagrama de vinculación

```text
Dispositivo paciente
  -> crear familia y membresía patient
  -> subir snapshot local completo
  -> crear código temporal
  -> compartir código

Dispositivo cuidador
  -> introducir código
  -> validar y consumir código
  -> crear membresía caregiver
  -> guardar contexto familiar
  -> backfill histórico + listeners
  -> Room local actualizado
```

## 5. Flujo técnico local -> SyncQueue -> Firebase -> otro dispositivo

### Escritura local y encolado

1. Una pantalla invoca una acción del ViewModel.
2. El ViewModel valida los datos.
3. El ViewModel llama al repositorio correspondiente.
4. El repositorio escribe primero en Room o DataStore.
5. Si la entidad debe sincronizarse, el repositorio llama a un método `enqueue*` de `FirebaseSyncManager`.
6. Se crea un `SyncQueueEntity` con:
   - Tipo de entidad.
   - ID de entidad.
   - Operación.
   - Payload JSON.
   - Estado `PENDING`.
   - Contador de reintentos.
7. Después de encolar, se intenta ejecutar `syncPendingNow()`.

### Procesamiento de SyncQueue

`syncPendingNow()` solo procesa la cola cuando:

- Firebase está configurado.
- Existe conexión a internet.
- La sincronización está activa.
- Existe un `familyId`.

Por cada operación `PENDING` o `FAILED`:

1. Cambia su estado a `SYNCING`.
2. Interpreta el tipo y la operación.
3. Escribe en Firestore o Firebase Storage.
4. Si termina correctamente, marca `SYNCED`.
5. Si falla, marca `FAILED`, incrementa `retryCount` y conserva el error.

Los registros `SYNCED` antiguos se eliminan después de tres días. El estado de última sincronización solo se actualiza cuando no hubo fallos.

### Tipos principales sincronizados

- Paciente.
- Medicamento.
- Lectura de presión.
- Log de medicamento.
- Rangos médicos.
- Preferencias de recordatorio y voz.
- Contacto familiar.
- Alertas.

### Recepción en el otro dispositivo

1. Los listeners observan cambios en Firestore.
2. Los datos remotos se convierten a entidades locales.
3. Los listeners escriben directamente en los DAOs.
4. No utilizan los repositorios de escritura local, evitando volver a encolar el mismo cambio.
5. Room emite los cambios a los `Flow`.
6. Los ViewModels actualizan las pantallas.

Antes de conectar los listeners recientes, se realiza un backfill paginado del histórico de presión y logs de medicamentos.

### Diagrama técnico

```text
Pantalla
  -> ViewModel
  -> Repository
  -> Room / DataStore
  -> FirebaseSyncManager.enqueue*
  -> SyncQueue
       PENDING
         -> SYNCING
           -> SYNCED
           -> FAILED -> reintento
  -> Firestore / Firebase Storage
  -> listeners del otro dispositivo
  -> DAO / archivos locales
  -> Room Flow
  -> ViewModel
  -> Pantalla actualizada
```

## 6. Flujo de medicamentos con imagen

### Creación o edición local

1. El cuidador abre medicamentos.
2. Registra nombre, dosis, hora, instrucciones y programación.
3. Opcionalmente toma una foto o selecciona una imagen de galería.
4. La imagen se copia al almacenamiento privado administrado por la aplicación.
5. Se construye o actualiza el `MedicationEntity`.
6. El medicamento se guarda en Room.
7. Se encola una operación de medicamento.
8. Se reprograman todos los recordatorios.

### Operaciones de imagen

La sincronización usa tres operaciones explícitas:

- `KEEP`: conserva la imagen remota existente.
- `UPLOAD`: sube o reemplaza la imagen.
- `DELETE`: elimina explícitamente la imagen.

Desactivar un medicamento utiliza soft-delete y conserva su imagen. Quitar la imagen explícitamente utiliza `DELETE`.

### Subida remota

1. `syncPendingNow()` procesa el medicamento.
2. Para `UPLOAD`, valida que exista el archivo local administrado.
3. Sube el archivo a Firebase Storage usando el path estable:

```text
families/{familyId}/patients/{patientId}/medications/{medicationId}.jpg
```

4. Después escribe el medicamento en Firestore con `imagePath`.
5. Para `DELETE`, elimina primero la imagen remota y actualiza Firestore.
6. Para `KEEP`, omite cambios sobre `imagePath`.

### Descarga en otro dispositivo

1. El listener recibe el medicamento y obtiene primero la versión local.
2. Si existe un `imagePath`, descarga la imagen a un archivo temporal.
3. Solo después de una descarga completa confirma el archivo en almacenamiento local.
4. Actualiza `imageUri` directamente mediante `MedicationDao`.
5. Si la descarga falla, conserva la imagen local anterior.
6. Si el remoto elimina explícitamente `imagePath`, elimina la imagen local administrada.
7. Si el documento remoto es legacy y no contiene información de imagen, conserva la imagen local.

Solo los dispositivos cuyo rol no es cuidador reprograman recordatorios al recibir medicamentos remotos.

## 7. Flujo de presión arterial

### Registro actual por el paciente

1. El paciente abre `Medir presión`.
2. Introduce presión sistólica, diastólica, pulso opcional y notas.
3. El ViewModel valida:
   - Sistólica entre 50 y 250.
   - Diastólica entre 30 y 160.
   - Pulso opcional entre 30 y 220.
4. `PressureRepository` consulta los rangos médicos locales.
5. Clasifica la lectura.
6. Inserta `BloodPressureEntity` en Room.
7. Encola una operación `CREATE`.
8. Muestra la pantalla de confirmación.

### Registro histórico por el cuidador

El cuidador puede registrar una lectura con fecha y hora manuales. El flujo valida que:

- La fecha no sea futura.
- La fecha pertenezca a los últimos treinta días.
- Los valores estén dentro de los rangos de entrada aceptados.

La lectura utiliza el mismo repositorio y el mismo flujo de sincronización.

### Sincronización y alertas

1. La lectura se sube a Firestore.
2. Si su estado es `HIGH`, `CRITICAL` u `OUT_OF_RANGE`, se crea una alerta remota.
3. El listener del otro dispositivo inserta o actualiza la lectura local.
4. El historial remoto completo se recupera mediante backfill paginado.
5. El listener reciente mantiene las últimas cincuenta lecturas.

### Eliminación

1. El historial solicita eliminar una lectura.
2. Se encola primero una operación `DELETE`.
3. Después se elimina la lectura de Room.
4. Firebase elimina el documento remoto.
5. Los otros dispositivos procesan el evento `REMOVED`.
6. Antes de borrar localmente, verifican contra servidor que el documento realmente ya no exista.

## 8. Flujo de logs de medicamentos

Los logs representan confirmaciones de tomas. Se crean principalmente desde:

- Inicio del paciente.
- Pantalla de recordatorio.
- Acción de notificación.
- Respuesta por voz.

### Registro individual

1. Se verifica que el medicamento corresponda al día actual.
2. Se calcula `scheduledFor` usando la hora programada.
3. Se comprueba que no exista ya un log para el mismo medicamento y horario.
4. Se crea un `MedicationLogEntity` con estado `TAKEN`.
5. Se inserta en Room.
6. Se encola para Firebase.

### Registro por grupo

1. Se obtienen los medicamentos pendientes para una hora.
2. Se excluyen los medicamentos ya confirmados.
3. Se crea un log por cada medicamento pendiente.
4. Los logs se insertan en lote.
5. Cada log se encola para sincronización.

### Historial y sincronización

- `DailyStatusRepository` combina medicamentos, logs del día y presión para construir el estado diario.
- `HistoryViewModel` combina medicamentos y logs para mostrar tomas y pendientes según el periodo seleccionado.
- Los logs se sincronizan como documentos individuales.
- El otro dispositivo recibe los logs mediante backfill paginado y un listener reciente limitado a doscientos documentos.
- El historial inicial local se sube mediante paginación keyset y batches Firestore.

El estado `MISSED` de los intentos de recordatorio pertenece a las entidades de recordatorios; no crea automáticamente un `MedicationLogEntity`.

## 9. Flujo de backup/importación

### Exportación

1. El usuario abre backup y elige un destino mediante Storage Access Framework.
2. `BackupRepository` consulta Room y DataStore.
3. Reúne:
   - Paciente.
   - Contacto familiar.
   - Rangos médicos.
   - Medicamentos.
   - Logs de medicamentos.
   - Lecturas de presión.
   - Preferencias.
   - Imágenes locales válidas.
4. Crea un ZIP con `backup.json` y las imágenes.
5. Informa cantidades exportadas y advertencias.

### Lectura previa a importación

1. El usuario selecciona un ZIP.
2. La aplicación copia el archivo a un temporal.
3. Valida encabezado, versión, estructura y datos.
4. Sanitiza el contenido.
5. Muestra un resumen antes de importar.

### Importación `MERGE`

- Conserva entidades locales más recientes.
- Evita duplicados por ID y, para presión, por firma.
- Inserta únicamente las entidades aceptadas.
- Restaura imágenes disponibles.
- Construye un plan de sincronización con lo realmente aplicado.

### Importación `REPLACE_ALL`

1. Cancela los recordatorios actuales.
2. Restaura imágenes en archivos administrados.
3. Dentro de una transacción Room:
   - Elimina recordatorios, logs, presiones, medicamentos, ajustes, contacto y paciente.
   - Inserta los datos restaurados.
4. Aplica preferencias restauradas.
5. Elimina imágenes locales antiguas que ya no correspondan.
6. Construye un plan de sincronización con las entidades restauradas.

### Resincronización posterior

1. El plan se convierte en operaciones durables de `SyncQueue`.
2. Las operaciones se insertan como lote.
3. Se ejecuta una sola sincronización después del encolado.
4. Las entidades mutables restauradas usan `skipIfRemoteNewer`.
5. Si el remoto tiene un `updatedAt` posterior, se omite la escritura restaurada.
6. Finalmente se reprograman los recordatorios.

## 10. Flujo de recordatorios

### Configuración

Las preferencias se guardan en DataStore e incluyen:

- Recordatorios activos.
- Intervalo entre repeticiones.
- Cantidad máxima de intentos.
- Sonido.
- Vibración.
- Aviso al cuidador por toma perdida.
- Recordatorios por voz.
- Cantidad de repeticiones de voz.

Los cambios de preferencias se encolan para Firebase. El dispositivo cuidador mantiene sus alarmas locales desactivadas durante el onboarding.

### Programación

1. `MedicationReminderScheduler` delega en `MedicationAlarmScheduler`.
2. Si los recordatorios están desactivados, cancela todas las alarmas.
3. Obtiene medicamentos activos.
4. Calcula su próxima ocurrencia.
5. Agrupa medicamentos que corresponden a la misma fecha y hora.
6. Guarda un `MedicationReminderEntity` por grupo.
7. Programa una alarma con `AlarmManager`.
8. Usa alarma exacta cuando el sistema lo permite y alarma aproximada como alternativa.

Los recordatorios se reprograman después de:

- Crear, editar o desactivar medicamentos.
- Restaurar un backup.
- Recibir medicamentos remotos en el dispositivo paciente.
- Reiniciar el dispositivo.
- Cambiar ajustes relevantes.

### Activación y respuesta

1. `MedicationAlarmReceiver` recibe la alarma.
2. Verifica que el recordatorio siga pendiente.
3. Comprueba que el grupo de medicamentos siga activo y pendiente.
4. Muestra la notificación.
5. Opcionalmente inicia voz y abre `ReminderActivity`.
6. Programa el siguiente intento o la comprobación final de toma perdida.

El paciente puede:

- Confirmar la toma.
- Posponer el recordatorio.
- Confirmar o posponer por voz.
- Pedir ayuda.

Al finalizar todos los intentos sin confirmación:

1. El grupo se marca `MISSED`.
2. Si la preferencia está activa, se encola una alerta para el cuidador.

## 11. Flujo de reportes

Los reportes se construyen exclusivamente desde datos locales en Room.

1. El cuidador abre `Reporte para el médico`.
2. Selecciona:
   - Últimos siete días.
   - Últimos treinta días.
   - Todo el historial.
3. `MedicalReportRepository` consulta:
   - Paciente.
   - Contacto familiar.
   - Rangos médicos.
   - Medicamentos activos.
   - Lecturas de presión del periodo.
   - Logs de medicamentos del periodo.
4. Calcula:
   - Resumen de presión.
   - Promedios y valores fuera de rango.
   - Medicamentos activos.
   - Tomas registradas.
   - Tomas pendientes u omitidas.
   - Porcentaje de adherencia.
5. La pantalla muestra una vista previa.
6. `PdfReportGenerator` genera un PDF local.
7. El usuario puede:
   - Preparar el reporte en caché.
   - Compartirlo mediante un intent Android.
   - Guardarlo mediante Storage Access Framework.

La generación del reporte no escribe en Firebase ni modifica datos clínicos.

## 12. Archivos principales por módulo

### Navegación y modos

- `android/app/src/main/java/com/cuidavoz/mobile/ui/navigation/AppNavigation.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/ui/navigation/ContigoDestination.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/ui/screens/PatientHomeScreen.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/ui/screens/caregiver/CaregiverDashboardScreen.kt`

### Onboarding y vinculación

- `android/app/src/main/java/com/cuidavoz/mobile/ui/screens/OnboardingScreen.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/ui/viewmodel/OnboardingViewModel.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/repository/OnboardingRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/ui/screens/caregiver/LinkCaregiverScreen.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/ui/viewmodel/CaregiverDashboardViewModel.kt`

### Sincronización Firebase

- `android/app/src/main/java/com/cuidavoz/mobile/data/sync/FirebaseSyncManager.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/sync/SyncContextRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/sync/ContigoMessagingService.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/firebase/FirestorePaths.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/firebase/FirestorePatientRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/firebase/FirestoreMedicationRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/firebase/FirestorePressureRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/firebase/FirestoreMedicationLogRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/firebase/FirestoreHealthSettingsRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/firebase/FirebaseStorageRepository.kt`

### Room y DAOs

- `android/app/src/main/java/com/cuidavoz/mobile/data/local/ContigoDatabase.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/local/PatientDao.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/local/MedicationDao.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/local/BloodPressureDao.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/local/MedicationLogDao.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/local/MedicationReminderDao.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/local/HealthSettingsDao.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/local/FamilyContactDao.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/local/SyncQueueDao.kt`

### Repositorios locales principales

- `android/app/src/main/java/com/cuidavoz/mobile/data/repository/PatientRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/repository/MedicationRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/repository/PressureRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/repository/MedicationLogRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/repository/DailyStatusRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/repository/MedicationReminderRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/repository/SettingsRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/repository/FamilyContactRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/repository/MedicalReportRepository.kt`

### Medicamentos e imágenes

- `android/app/src/main/java/com/cuidavoz/mobile/ui/viewmodel/MedicationsViewModel.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/ui/screens/MedicationsScreen.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/files/MedicationImageStorage.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/domain/sync/MedicationImageSyncOperation.kt`

### Presión, logs e historial

- `android/app/src/main/java/com/cuidavoz/mobile/ui/viewmodel/HomeViewModel.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/ui/viewmodel/HistoricalPressureViewModel.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/ui/viewmodel/HistoryViewModel.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/ui/screens/MeasurePressureScreen.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/ui/screens/HistoryScreen.kt`

### Recordatorios

- `android/app/src/main/java/com/cuidavoz/mobile/reminders/MedicationReminderScheduler.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/reminders/MedicationAlarmScheduler.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/reminders/MedicationAlarmReceiver.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/reminders/MedicationReminderActionHandler.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/reminders/ReminderActivity.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/reminders/ReminderPreferencesRepository.kt`

### Backup y reportes

- `android/app/src/main/java/com/cuidavoz/mobile/data/backup/BackupRepository.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/backup/BackupRestoreSyncPlan.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/ui/viewmodel/BackupViewModel.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/ui/viewmodel/ReportsViewModel.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/report/PdfReportGenerator.kt`
- `android/app/src/main/java/com/cuidavoz/mobile/data/repository/MedicalReportRepository.kt`

## 13. Pendientes relacionados con M2/M3/M4

Esta sección resume únicamente los pendientes ya registrados en `docs/auditoria-logica-progreso.md`.

### M2: sincronización inicial y listeners limitados

Estado: corregido en código; pendiente de validación real.

Pendientes:

- Ejecutar pruebas específicas de las fases de backfill y subida inicial contra Firebase Emulator.
- Realizar una prueba multidispositivo que confirme la descarga y subida completa del histórico.
- Evaluar el tiempo de vinculación con históricos locales o remotos muy grandes.
- Considerar que los documentos legacy sin `measuredAt` o `scheduledAt` no participan en consultas ordenadas.

### M3: sincronización de imágenes de medicamentos

Estado: validado manualmente con Firebase Emulator y dos dispositivos reales el 10 de junio de 2026; backfill legacy pendiente.

Pendientes:

- Implementar backfill para imágenes locales creadas antes de M3 Fase 2.
- Resolver posibles operaciones `UPLOAD` antiguas fallidas después de múltiples cambios offline.
- Incorporar hash o versión de imagen para evitar descargas redundantes.
- Mejorar el reintento cuando fallan todos los intentos acotados de descarga.
- Evaluar desacoplar imagen y medicamento en el modelo de sincronización.

### M4: resincronización después de importar backup

Estado: corregido en código; pendiente de validación real.

Pendientes:

- Probar el flujo completo contra Firebase Emulator y un segundo dispositivo.
- Las preferencias restauradas no se resincronizan porque no tienen un `updatedAt` fiable.
- `REPLACE_ALL` no elimina documentos remotos ausentes en el backup.
- La restauración local y la creación de la cola post-import no forman una única transacción atómica.
- Mantener bajo observación la resolución de conflictos basada en `updatedAt`.

