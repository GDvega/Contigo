# Recordatorios hablados Android por fases

## Objetivo

Agregar recordatorios hablados confiables para medicamentos en Android sin romper la app Expo actual ni forzar una migracion completa.

## Estado actual

- `expo-notifications` programa los recordatorios locales.
- `expo-speech` reproduce voz cuando la app esta abierta.
- El hablado real en segundo plano o con la app cerrada no esta garantizado por el flujo JS actual.

## Fase 1

- Mantener el backend actual de notificaciones.
- Introducir una capa de compatibilidad para recordatorios hablados.
- Exponer en la UI el nivel real de soporte para no prometer voz en background cuando aun no existe.

Resultado:

- No cambia el comportamiento estable ya existente.
- La app queda lista para enchufar un backend nativo Android sin reescribir pantallas o logica de medicamentos.

## Fase 2

- Crear un modulo nativo Android dedicado, por ejemplo `CuidaVozSpokenReminders`.
- Ese modulo debe recibir el payload del recordatorio y decidir si usa:
  - `AlarmManager` para el disparo preciso.
  - `BroadcastReceiver` para reaccionar al horario.
  - `TextToSpeech` nativo para hablar.
  - `ForegroundService` solo cuando Android lo exija para ejecutar audio de forma confiable.
- Mantener `expo-notifications` para la notificacion visible y accion del usuario.

Requisitos recomendados:

- Permiso `RECEIVE_BOOT_COMPLETED` para restaurar alarmas tras reinicio.
- Evitar servicios persistentes innecesarios.
- Respetar Doze y restricciones modernas de Android 12+.
- No usar playback continuo ni hacks de background agresivos.

## Fase 3

- Agregar acciones nativas en la notificacion:
  - "Tomado"
  - "Recordar luego"
- Sincronizar esas acciones con SQLite local para que el estado diario quede consistente aunque la app no este abierta.

## Fase 4

- Restaurar alarmas al reiniciar el telefono.
- Reprogramar automaticamente cuando cambien medicamentos, horarios o zona horaria.
- Añadir observabilidad minima:
  - ultimo disparo hablado
  - ultimo error nativo
  - backend activo

## Contrato JS previsto

La app ya consulta una capa intermedia en `src/lib/spokenReminders.ts`.

Contrato esperado para el modulo nativo futuro:

- `isAvailable(): boolean`
- `speakReminderNow(payload): void | Promise<void>`

Payload minimo:

- `title`
- `message`
- `scheduleTime`
- `medicationNames`

## Criterio de seguridad

- Si el modulo nativo no existe, CuidaVoz debe degradar al modo actual sin fallar.
- La logica de confirmacion de toma y programacion de recordatorios debe seguir viviendo en el dominio actual de la app.
- La fase nativa debe ser incremental y activable sin migrar toda la app fuera de Expo.
