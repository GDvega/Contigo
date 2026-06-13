# Prompt detallado — Contigo

Documento de referencia del producto: identidad, público, funciones, arquitectura y tono. Útil como contexto maestro para diseño, documentación, marketing o desarrollo con IA.

**Última actualización:** junio 2026 — refleja el renombrado a **Contigo**, notificaciones mejoradas, formato de backup y estado actual del código Android.

---

## Identificadores técnicos (marca vs código)

| Concepto | Valor actual |
|----------|--------------|
| **Marca visible** | Contigo |
| **Monorepo** | `cuida-voz/` (nombre histórico del repositorio) |
| **Package Android** | `com.cuidavoz.mobile` (sin cambiar por compatibilidad de Play Store) |
| **Versión APK** | 1.0.0 (`versionCode` 1) |
| **SDK** | minSdk 26, targetSdk 35, compileSdk 35 |
| **Clases internas** | `ContigoApp`, `ContigoDatabase`, `ContigoDestination`, `ContigoBackup`, `ContigoLog`, etc. |
| **Respaldos antiguos** | ZIPs exportados como **CuidaVoz** siguen importándose; nuevos usan marca **Contigo** |

---

## Identidad del producto

**Contigo** es una aplicación de salud domiciliaria orientada a personas mayores y a sus familiares o cuidadores. Ayuda a **recordar medicamentos**, **registrar presión arterial**, **pedir ayuda** y **compartir información** con un familiar, sin exigir conocimientos técnicos.

El nombre refleja compañía y cercanía: **interacción por voz** (recordatorios hablados, comandos de voz, guía audible) combinada con **botones grandes y pantallas simples** para quienes prefieren tocar en lugar de hablar.

**Plataformas del ecosistema:**

- **Android:** app nativa Kotlin + Jetpack Compose, uso diario en el teléfono del paciente o del cuidador.
- **Firebase:** sincronización entre dispositivos (Firestore, Auth anónimo, FCM, Storage para imágenes).

---

## Público objetivo

### Usuario principal — Paciente

- Adultos mayores o personas con **baja alfabetización digital**.
- Pueden vivir solos o con apoyo familiar limitado durante el día.
- Necesitan recordatorios claros de pastillas y una forma sencilla de registrar presión y pedir ayuda.
- Pueden tener dificultad para leer textos pequeños o navegar menús complejos.

### Usuario secundario — Cuidador / familiar

- Hijo/a, cónyuge, enfermera o persona de confianza que **configura** medicamentos, revisa adherencia, genera reportes para el médico y se vincula con el celular del paciente.
- Suele ser más cómodo con tecnología, pero también valora interfaces claras.

### Usuario terciario — Profesional de salud (indirecto)

- No usa la app directamente en la versión actual; recibe **reportes PDF** generados por el cuidador con historial de presión y tomas.

---

## Problema que resuelve

| Necesidad | Cómo lo aborda Contigo |
|-----------|-------------------------|
| Olvidar pastillas | Alarmas exactas, notificaciones agrupadas, pantalla de recordatorio, reintentos configurables |
| No saber si la presión está bien | Registro manual o por voz + clasificación (normal, elevada, alta, crítica, fuera de rango personalizado) |
| Emergencia o soledad | Contacto familiar con llamada/mensaje desde «Pedir ayuda» |
| Cuidador lejos del paciente | Sincronización Firebase entre dos celulares vinculados por código alfanumérico de 10 caracteres |
| Pérdida de datos al cambiar teléfono | Copia de seguridad local cifrada en archivo ZIP (exportar/importar con contraseña) |
| Barrera digital | Modo fácil, tipografía grande, guía por voz, asistente de voz en español |

---

## Modelo de uso: dos roles en un solo APK

Al **primer ingreso (onboarding)** el usuario elige:

1. **«Soy paciente»** — el celular se configura como dispositivo del paciente.
2. **«Soy cuidador o familiar»** — orientado a seguir la salud de otra persona y vincularse con su celular.

**Defaults al completar onboarding del paciente:**

- Modo fácil, guía por voz y voz en recordatorios **activados**.
- Repeticiones de voz en recordatorio: **2**.
- Rangos de presión por defecto creados si no existen.
- Recordatorios activados (según elección en onboarding).

**Defaults al completar onboarding del cuidador:**

- Modo fácil y recordatorios **desactivados** (este celular no es el del paciente).

El rol se puede **cambiar después** entrando al «área del familiar/cuidador» desde la pantalla del paciente (con diálogo de confirmación para evitar cambios accidentales).

**Dos modos visuales (temas):**

- **Modo paciente:** fondo cálido, botones grandes, foco en acciones del día (pastilla, presión, ayuda, voz).
- **Modo cuidador:** panel de gestión con acceso a medicamentos, registros, reportes, vinculación, sincronización y copias.

La app **no siembra datos demo**; `LegacyDemoDataCleaner` elimina datos de versiones antiguas.

---

## Arquitectura de navegación (Android)

Pantallas principales (`ContigoDestination`, Navigation Compose type-safe):

| Pantalla | Rol típico | Función |
|----------|------------|---------|
| **Onboarding** | Ambos | Elección de rol, datos del paciente, contacto familiar, activación de recordatorios, importación opcional de backup |
| **PatientHome** | Paciente | Inicio: saludo, estado del día, próxima pastilla, presión, recordatorio activo, botones principales |
| **MeasurePressure** | Paciente | Registrar sistólica, diastólica y pulso opcional |
| **PressureSaved** | Paciente | Confirmación tras guardar presión |
| **Help** | Paciente | Llamar o enviar mensaje al contacto familiar |
| **ReminderActivity** | Paciente | Activity full-screen al abrir notificación (no es ruta Compose); 3 botones grandes; mantiene pantalla encendida |
| **CaregiverHome** | Cuidador | Dashboard: resumen paciente, sincronización, accesos rápidos |
| **LinkCaregiver** | Ambos* | Crear código (paciente) o ingresar código (cuidador) para vincular familias en Firebase |
| **Medications** | Cuidador | CRUD de medicamentos con foto, horario, tratamiento flexible |
| **Records** | Cuidador | Historial de presión y tomas |
| **HistoricalPressure** | Cuidador | Carga/visualización de históricos de presión |
| **Reports** | Cuidador | Generación de reporte médico PDF (7 / 30 días / todo) |
| **FamilyContact** | Cuidador | Editar contacto de emergencia |
| **Settings** | Cuidador | Recordatorios, rangos de presión, voz, modo fácil, permisos |
| **Backup** | Cuidador | Exportar/importar ZIP con todos los datos locales |

\*La vinculación hoy se accede sobre todo desde el panel del cuidador; el flujo ideal es que el **paciente genere el código** y el **cuidador lo ingrese**.

**Guía por voz por pantalla:** si `voiceGuidanceEnabled`, al entrar en cada destino se lee un texto corto definido en `ContigoDestination.voiceGuideText()`.

---

## Funcionalidades del paciente (detalle)

### Pantalla de inicio (`PatientHome`)

- **Saludo personalizado** con el nombre del paciente.
- **Píldora de estado** del día: «Todo en orden», «Hay pendientes» o «Revisar con familiar o médico» (según presión y pastillas pendientes).
- **Tarjeta de presión:** última medición del día o «Sin registro hoy».
- **Tarjeta de medicamento principal:** nombre(s), hora, imágenes de las pastillas si existen.
- **Recordatorio en pantalla** si hay un aviso pendiente: botones **Ya tomé** y **Posponerlo**.
- **Botón «Ya tomé»** cuando hay pastillas pendientes sin prompt activo.
- **Medir presión** — acceso directo.
- **Pedir ayuda** — abre pantalla de contacto.
- **Asistente de voz** (`VoiceAssistantSection`): botón «Hablar con Contigo»; reconoce frases en español coloquial.
- **Familiar / Ajustes** — entrada protegida al área del cuidador.

### Registro de presión

- Campos numéricos grandes para presión sistólica, diastólica y pulso opcional.
- Clasificación automática con `PressureClassifier`:

| Prioridad | Condición | Estado |
|-----------|-----------|--------|
| 1 | Sistólica ≥ 180 o diastólica ≥ 120 | **CRITICAL** |
| 2 | Sistólica ≥ 140 o diastólica ≥ 90 | **HIGH** |
| 3 | Fuera de rangos personalizados en ajustes | **OUT_OF_RANGE** |
| 4 | Sistólica ≥ 120 | **ELEVATED** |
| 5 | Resto | **NORMAL** |

- Mensajes de seguridad no alarmistas pero claros para valores altos o fuera de rango.
- Posibilidad de registrar **por voz** («tengo 120 sobre 80», números hablados, etc.).

### Pedir ayuda (`Help`)

- Muestra nombre y relación del contacto familiar.
- **Llamar** (abre marcador) y **enviar mensaje** (SMS/WhatsApp según disponibilidad).
- Si no hay contacto, orienta a configurarlo en el área del cuidador.

### Asistente de voz (paciente)

Intenciones soportadas (`VoiceIntentParser`):

- Registrar o iniciar flujo de **presión** (incluye extracción de valores hablados).
- Confirmar **una pastilla** o **todas las del grupo actual**.
- **Pedir ayuda** / emergencia / llamar al familiar.
- **Repetir** el recordatorio.
- **Cancelar** o corregir lo dicho.

**Español coloquial reconocido:** pastilla, remedio, jarabe, gotas, pomada, inyección; correcciones («me equivoqué», «digo», «olvidalo»); variantes regionales en números hablados.

Flujo con **confirmación por voz** antes de acciones sensibles (presión, medicación, ayuda). Si falla el reconocimiento o faltan permisos, ofrece **usar botones** en lugar de voz.

### Recordatorios de medicación

- **Alarmas exactas** (`SCHEDULE_EXACT_ALARM`) para horarios puntuales.
- **Notificación** con acciones: «Ya tomé» / «Posponerlo»; al abrir, **ReminderActivity** con solo **3 botones grandes**:
  1. **Ya tomé** (o «Ya tomé todas»)
  2. **Posponerlo**
  3. **Hablar** (micrófono; puede decir «ya tomé» o «posponerlo»)
- **Texto a voz** del recordatorio (repetible N veces según ajustes).
- **Reintentos:** intervalo y máximo de avisos configurables; reintentos usan `setOnlyAlertOnce` para no repetir sonido/vibración innecesariamente.
- **Snooze** reprograma el recordatorio.
- Servicio en primer plano para voz en recordatorio (`MedicationReminderVoiceService`).
- Tras reinicio del dispositivo, `BOOT_COMPLETED` reprograma alarmas.
- Si la app está abierta al disparar el recordatorio, navega a `PatientHome`.

### Notificaciones (Android)

Tres canales separados (`MedicationNotificationChannels`):

| Canal | Importancia | Uso |
|-------|-------------|-----|
| **Recordatorios de pastillas** | Alta | Avisos de horario con sonido y vibración |
| **Confirmaciones** | Default | Respuestas breves al marcar toma o posponer (sin sonido) |
| **Lectura en voz alta** | Baja | Servicio foreground mientras Contigo lee el recordatorio |

- Recordatorios usan **MessagingStyle** con persona «Contigo» y agrupación (`MEDICATION_REMINDER_GROUP_KEY`).
- Icono de notificación: `ic_stat_contigo`; color primario `#0F6B6E`.

### Agrupación de medicamentos

- Varias pastillas a la **misma hora** se agrupan en un solo recordatorio («Ya tomé todas»).
- El estado diario calcula grupos pendientes, tomados y próximo horario.

---

## Funcionalidades del cuidador (detalle)

### Panel del cuidador (`CaregiverDashboard`)

- Nombre del paciente, última presión, estado textual, pastillas pendientes hoy.
- **Últimas tomas** recientes.
- **Sincronización:** estado (con/sin internet), si el celular está vinculado a una familia, interruptor activar/pausar sync, botones «Vincular paciente» y «Sincronizar ahora».
- Accesos: Medicamentos, Registros, Histórico presión, Reporte médico, Contacto familiar, Ajustes, Copia de seguridad.
- Volver al modo paciente o «Llamar al paciente» (atajo a ayuda).

### Gestión de medicamentos (`MedicationsScreen`)

Por cada medicamento:

- Nombre, dosis, color, forma, instrucciones.
- **Hora** del recordatorio (selector de hora).
- **Foto** de la pastilla (cámara o galería; subida a Firebase Storage si hay sync).
- **Tipos de tratamiento:**
  - Todos los días (sin fin)
  - Rango de fechas (inicio–fin)
  - Días de la semana
  - Fechas específicas
  - Atajos de «pocos días» (3, 5, 7, 10, 14, 30)
- Activar/desactivar, editar, eliminar.
- En **modo fácil** se ocultan opciones avanzadas de la UI de medicamentos.

### Registros e histórico

- **Records:** listado de lecturas de presión y logs de medicación (tomado, omitido, pendiente).
- **HistoricalPressure:** revisión/carga de series de presión para seguimiento longitudinal.

### Reporte médico (`Reports` + PDF)

- Períodos: últimos 7 días, 30 días o todo el historial.
- Incluye: resumen de presión, estado de medicamentos, adherencia, tratamientos activos/vencidos.
- Generación de **PDF** (`PdfReportGenerator`) con pie «Generado por Contigo».

### Contacto familiar (`FamilyContactScreen`)

- Nombre, teléfono (validado/formateado), relación (ej. «Hijo», «Cuidador»).
- Usado en «Pedir ayuda» y en flujos de voz de emergencia.

### Ajustes (`SettingsScreen`)

- **Modo fácil:** botones más grandes, menos texto, más ayuda por voz.
- **Guía por voz:** lee instrucciones al entrar en cada pantalla.
- **Recordatorios:** activar/desactivar, intervalo de repetición, máximo de avisos, sonido, vibración, avisar al cuidador si se omite toma.
- **Voz en recordatorios:** activar TTS en alarmas, cantidad de repeticiones.
- **Asistente de voz** global.
- **Rangos de presión** personalizados (mín/máx sistólica y diastólica).
- Permisos: notificaciones, micrófono, alarmas exactas (enlace a ajustes del sistema si falta).
- Reprogramar todos los recordatorios.

### Copia de seguridad (`BackupScreen`)

**Formato ZIP (`ContigoBackup`, versión 1):**

```
contigo-backup/
├── backup.json          # metadatos y datos
└── images/              # fotos de medicamentos
```

- Nombre de exportación: `contigo-backup-YYYYMMDD-HHmmss.zip`
- Marca en JSON: `"app": "Contigo"`; también acepta respaldos `"CuidaVoz"` (compatibilidad hacia atrás).
- **`allowBackup=false`** en el manifest: no hay backup automático de Android; solo el ZIP manual.

**Contenido exportado:**

- Paciente, contacto familiar, ajustes de salud.
- Medicamentos, logs de toma, lecturas de presión.
- Preferencias: recordatorios, voz, modo fácil, guía por voz.
- Imágenes de medicamentos embebidas en el ZIP.

**Importación con vista previa (`BackupSummary`) y dos estrategias:**

| Estrategia | Comportamiento |
|------------|----------------|
| **REPLACE_ALL** | Borra datos locales y restaura todo desde el ZIP |
| **MERGE** | Fusiona por ID; medicamentos más recientes (`updatedAt`) prevalecen; omite duplicados de presión y logs |

Disponible también en **onboarding** para restaurar un dispositivo nuevo.

### Vinculación paciente ↔ cuidador (Firebase)

1. En un celular (idealmente el del **paciente**): **Crear código** alfanumérico de 10 caracteres (válido ~10 minutos; códigos legacy de 6 dígitos aún válidos).
2. Se crea/actualiza una **familia** en Firestore (`families/{familyId}`) con miembro `patient`.
3. En el celular del **cuidador**: ingresar código → transacción que añade miembro `caregiver`, asigna `mainCaregiverId` al paciente remoto y consume el código.
4. Ambos dispositivos sincronizan medicamentos, presión, logs, alertas y preferencias vía cola offline + listeners en tiempo real.

**Auth:** Firebase Authentication **anónimo** por dispositivo.

**Offline:** la app funciona sin internet; los cambios se encolan en Room (`sync_queue`) y se envían cuando hay red y sync está activa.

---

## Cálculo del estado diario (`DailyStatusCalculator`)

Combina:

- Medicamentos activos hoy (respetando calendario de tratamiento).
- Logs de toma del día.
- Última presión del día y su `PressureStatus`.

Produce:

- Nivel de riesgo: **LOW / MEDIUM / HIGH**.
- Título: «Todo en orden», «Hay pendientes», «Revisar con familiar o médico».
- Próximo grupo de pastillas y conteos tomadas/pendientes.

---

## Modelo de datos local (Room)

Entidades principales:

- `PatientEntity` — perfil del paciente en este dispositivo.
- `MedicationEntity` — medicamento con horario y reglas de calendario.
- `BloodPressureEntity` — lecturas con status y timestamp.
- `MedicationLogEntity` — eventos de toma (TAKEN, etc.) por horario programado.
- `MedicationReminderEntity` — estado de alarmas/recordatorios programados.
- `HealthSettingsEntity` — rangos normales de presión.
- `FamilyContactEntity` — contacto de ayuda.
- `SyncQueueEntity` — cola de operaciones pendientes hacia Firestore.

Base de datos: `cuida_voz.db` (nombre histórico), versión 8, con migraciones. Un paciente local por dispositivo (`DEFAULT_PATIENT_ID` / `patient_primary`).

---

## Sincronización en la nube (Firebase)

**Colecciones Firestore (por familia):**

- `families/{familyId}` — metadatos de familia.
- `members/{userId}` — rol `patient` o `caregiver`.
- `patients/{patientId}/medications`, `pressureReadings`, `medicationLogs`, `healthSettings`, `alerts`, `contact`, `preferences/reminders`.
- `linkCodes/{code}` — códigos temporales de vinculación.

**Tipos sincronizados:** paciente, medicamentos, presión, logs, alertas, contacto, preferencias de recordatorio/voz, configuración de salud.

**FCM:** `ContigoMessagingService` para notificaciones push (p. ej. alertas al cuidador).

**Reglas de seguridad:** acceso basado en pertenencia a la familia y rol; códigos de enlace legibles por usuarios autenticados (área de mejora de seguridad documentada en auditorías).

---

## Diseño UX para adultos mayores

Principios implementados:

- **Tipografía grande** (títulos 30–40 sp, botones 24–28 sp).
- **Alto contraste** (verde azulado primario `#0F6B6E`, fondo crema `#FBF7EF`).
- **Botones de altura mínima** ~60–76 dp en acciones críticas.
- **Modo fácil** activado por defecto en onboarding del paciente.
- **Guía por voz** activada por defecto para pacientes.
- **Pocos botones por pantalla** en momentos críticos (recordatorio = 3 botones).
- **Iconografía** clara (corazón = presión, teléfono = ayuda, micrófono = voz).
- **Confirmaciones** antes de registrar tomas o presión por voz.
- **Mensajes en español** coloquial de Latinoamérica/España («pastilla», «remedio», «jarabe», etc. en el parser de voz).

Colores funcionales del tema:

- Éxito verde, error rojo, info azul.
- Botón de voz lavanda (`#E9DDF8`).
- Botón ayuda rosa suave, medir presión verde menta.

---

## Stack técnico (Android)

| Área | Tecnología |
|------|------------|
| Lenguaje | Kotlin 17 |
| UI | Jetpack Compose, Material 3 |
| Arquitectura | MVVM + Hilt (DI) |
| Persistencia | Room + DataStore (preferencias) |
| Navegación | Navigation Compose 2.8+ con rutas serializables |
| Imágenes | Coil |
| Voz | Android SpeechRecognizer + TextToSpeech propio |
| Alarmas | AlarmManager + BroadcastReceivers |
| Red | Firebase BOM (Firestore, Auth, Messaging, Storage) |
| Reportes | PDF on-device (`PdfReportGenerator`) |
| Logs | `ContigoLog` — debug/info/warn solo en builds debug; errores también en release |
| Tests | Unitarios (dominio, recordatorios, voz) + instrumentados (`OnboardingFlowTest` con `HiltTestRunner`) |
| CI | GitHub Actions con emulador API 34 (lint + unit + connected tests) |

**Permisos Android:** `POST_NOTIFICATIONS`, `CAMERA`, `RECORD_AUDIO`, `VIBRATE`, `WAKE_LOCK`, `SCHEDULE_EXACT_ALARM`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `INTERNET`, `RECEIVE_BOOT_COMPLETED`.

**Capas:** UI → ViewModel → dominio (puro) → repositorios / Room / sync. Ver `android/ARCHITECTURE.md`.

---

## Flujos de usuario típicos (narrativa)

### Día del paciente

1. A la hora de la pastilla suena la notificación y/o la voz.
2. Abre el aviso → pantalla simple con foto y nombre del medicamento.
3. Pulsa **Ya tomé** o dice «ya tomé» al micrófono.
4. Por la mañana registra presión (botón o voz).
5. Si se siente mal, **Pedir ayuda** → llama al hijo configurado.

### Día del cuidador

1. Revisa en el panel si hay pastillas pendientes o presión alta.
2. Añade un medicamento nuevo con foto y horario.
3. Exporta reporte PDF de la semana para la consulta médica.
4. Si cambió el teléfono del paciente, guía la **vinculación** con código de enlace o restaura un **backup ZIP cifrado**.

---

## Limitaciones y consideraciones (estado actual)

- **Un paciente local por dispositivo**; multi-paciente no es el foco del APK actual.
- **No es un dispositivo médico certificado**; mensajes orientan a consultar con familiar o profesional.
- **Vinculación** requiere Firebase configurado (`google-services.json`) y reglas desplegadas; auth anónimo habilitado.
- **Códigos de enlace** expiran en 10 minutos; seguridad de códigos mejorable (auditoría CV-AUD-006).
- **Pantalla de vinculación** accesible principalmente desde área cuidador (mejora UX pendiente para pacientes).
- La app **funciona offline**; sync es eventual.
- **Package `com.cuidavoz.mobile`** se mantiene por compatibilidad; solo la marca visible cambió a Contigo.

---

## Tono de comunicación del producto

- Cálido, respetuoso, **tuteo** cercano («Hola, María», «Te escucho», «Soy Contigo»).
- Evitar jerga médica innecesaria; preferir «pastilla», «presión», «familiar».
- Instrucciones **cortas y en una sola acción por frase**.
- Nunca culpar al usuario por errores de voz; ofrecer alternativa con botones.
- Refuerzo positivo al completar acciones («Listo. Toma registrada.»).

---

## Resumen ejecutivo (una línea)

> **Contigo** es una app Android (con Firebase) para **adultos mayores** que necesitan recordatorios de medicamentos y registro de presión arterial con **voz y botones grandes**, y para **familiares/cuidadores** que configuran tratamientos, revisan adherencia, generan reportes PDF, respaldan datos y **vinculan dos teléfonos** para sincronizar la salud del paciente de forma simple y en español.

---

## Prompt maestro compacto (copiar y pegar)

```markdown
# Contigo — Especificación de producto

## Qué es
Contigo es salud domiciliaria en español para adultos mayores y cuidadores: recordatorios de medicación con voz y alarmas exactas, registro de presión (manual o por voz), pedir ayuda al familiar, sync entre dos teléfonos vía Firebase, backup ZIP y panel del cuidador con PDF.

Ecosistema: Android Kotlin/Compose (v1.0.0, minSdk 26), Firebase (Firestore, Auth anónimo, FCM, Storage).

## Público
1. Paciente mayor, baja alfabetización digital, vive solo o con apoyo limitado.
2. Cuidador/familiar: configura medicamentos, revisa adherencia, reportes PDF, vincula celulares, respalda datos.
3. Médico (indirecto): recibe PDF del cuidador.

## Roles (un APK)
Onboarding: «Soy paciente» o «Soy cuidador». Paciente: modo fácil + guía por voz + TTS en recordatorios por defecto. Cuidador: panel de gestión. Paciente accede al área cuidador con confirmación.

## Pantallas clave
PatientHome, MeasurePressure, Help, ReminderActivity (3 botones), CaregiverHome, Medications, Records, Reports, Settings, Backup, LinkCaregiver.

## Voz
VoiceIntentParser: presión, confirmar toma(s), ayuda, repetir, cancelar. Español coloquial. Confirmación antes de acciones sensibles. Fallback a botones.

## Presión
PressureClassifier: CRITICAL ≥180/≥120, HIGH ≥140/≥90, OUT_OF_RANGE (personalizado), ELEVATED ≥120, NORMAL.

## Recordatorios
AlarmManager exacto, 3 canales de notificación, MessagingStyle, reintentos/snooze, TTS, servicio foreground de voz, BOOT_COMPLETED.

## Backup
ZIP contigo-backup/ con JSON + imágenes. Import: REPLACE_ALL o MERGE. Compatible con respaldos CuidaVoz antiguos.

## Datos
Room v8 (cuida_voz.db): paciente, medicamentos, presión, logs, recordatorios, contacto, health settings, sync_queue. Offline-first.

## UX mayores
Tipografía grande, botones 60–76 dp, primario #0F6B6E, fondo crema, tuteo cálido, pocas opciones en momentos críticos.

## Limitaciones
No dispositivo médico certificado. Un paciente por dispositivo. Firebase opcional para sync. Package interno com.cuidavoz.mobile.
```

---

## Documentos relacionados

- [README.md](../README.md) — estructura del monorepo y comandos de build
- [ARCHITECTURE.md](../android/ARCHITECTURE.md) — capas Android
- [FIREBASE_RULES.md](./FIREBASE_RULES.md) — reglas Firestore
- [AUDIT_REPORT.md](./AUDIT_REPORT.md) — auditoría técnica
- [QA_ENTERPRISE_AUDIT.md](./QA_ENTERPRISE_AUDIT.md) — checklist QA
