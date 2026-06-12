# QA Enterprise Audit Contigo

## A. Instalación limpia

- [ ] Instalar APK debug desde cero.
- [ ] Instalar APK release desde cero.
- [ ] Confirmar que abre sin crash en primer inicio.
- [ ] Confirmar idioma, permisos y navegación básica.

## B. Primer inicio

- [ ] Verificar si aparecen datos demo.
- [ ] Confirmar si eso es esperado para demo o si debe iniciar vacío.
- [ ] Confirmar creación de canal de notificación.
- [ ] Confirmar solicitud de permisos solo cuando corresponde.

## C. Modo paciente

- [ ] Abrir Inicio paciente sin leer demasiado.
- [ ] Confirmar botones grandes y textos simples.
- [ ] Confirmar que “Familiar / Ajustes” no confunde al paciente.
- [ ] Confirmar que “Escuchar” funciona.

## D. Medir presión

- [ ] Registrar presión válida.
- [ ] Intentar valores imposibles.
- [ ] Confirmar mensaje simple y no diagnóstico.
- [ ] Confirmar persistencia local sin internet.

## E. Medicamentos

- [ ] Crear medicamento sin imagen.
- [ ] Crear medicamento con foto desde cámara.
- [ ] Crear medicamento con foto desde galería.
- [ ] Editar medicamento.
- [ ] Desactivar medicamento.
- [ ] Confirmar que no queden imágenes huérfanas.

## F. Tratamientos por fecha/día

- [ ] `ALWAYS`
- [ ] `DATE_RANGE`
- [ ] `WEEKLY_DAYS`
- [ ] `SPECIFIC_DATES`
- [ ] Verificar que vencidos no aparezcan como pendientes.
- [ ] Verificar que martes no cuente como omitido si la pastilla era solo L/M/V.

## G. Recordatorio hablado con app cerrada

- [ ] Crear una pastilla para 2 minutos después.
- [ ] Cerrar la app por completo.
- [ ] Bloquear el celular.
- [ ] Esperar la alarma.
- [ ] Confirmar notificación visible.
- [ ] Confirmar vibración.
- [ ] Confirmar sonido.
- [ ] Confirmar TTS automático con nombre, dosis, hora y descripción.

## H. Repetición 5/10 min

- [ ] No responder al intento 1.
- [ ] Confirmar intento 2 al intervalo configurado.
- [ ] Confirmar intento 3 al intervalo configurado.
- [ ] Confirmar que no repite infinito.

## I. Confirmar desde notificación

- [ ] Tocar `Ya tomé`.
- [ ] Confirmar creación de log local.
- [ ] Confirmar cancelación de avisos siguientes del grupo.
- [ ] Confirmar sincronización posterior si hay internet.

## J. Alertar cuidador

- [ ] Tocar `Pedir ayuda`.
- [ ] Confirmar alerta local.
- [ ] Confirmar alerta remota cuando vuelva internet.
- [ ] Dejar expirar `maxAttempts` y verificar `missed_medication`.

## K. Firebase sync en dos celulares

- [ ] Vincular paciente y cuidador.
- [ ] Crear medicamento en un celular y verlo en el otro.
- [ ] Confirmar toma en paciente y verla en cuidador.
- [ ] Registrar presión y verla en cuidador.
- [ ] Confirmar que no se mezclan familias.

## L. Offline / online

- [ ] Crear datos sin internet.
- [ ] Reabrir con internet.
- [ ] Confirmar vaciado de SyncQueue.
- [ ] Confirmar ausencia de duplicados.

## M. Backup / restore

- [ ] Exportar respaldo.
- [ ] Validar advertencia de privacidad.
- [ ] Importar respaldo válido.
- [ ] Importar respaldo inválido.
- [ ] Verificar reprogramación de recordatorios después de restore.
- [ ] Verificar que restore no duplique sync remoto.

## N. Reporte médico

- [ ] Abrir reporte 7 días.
- [ ] Abrir reporte 30 días.
- [ ] Abrir reporte completo.
- [ ] Generar PDF con datos.
- [ ] Generar PDF sin datos.
- [ ] Guardar por SAF.
- [ ] Compartir por sharesheet.

## O. Voz manual

- [ ] Probar lectura manual.
- [ ] Probar respuesta por voz visible.
- [ ] Decir `ya tomé`.
- [ ] Confirmar diálogo visible antes de registrar.
- [ ] Decir una frase casual en tercera persona y confirmar rechazo.

## P. Seguridad / reglas

- [ ] Probar con Firebase Emulator si se agrega configuración real.
- [ ] Verificar que otro usuario autenticado no accede a otra familia.
- [ ] Verificar que `linkCodes` expiran y no pueden reutilizarse.
- [ ] Verificar que no se filtran datos médicos en logs.

## Q. Permisos

- [ ] POST_NOTIFICATIONS en Android 13+.
- [ ] RECORD_AUDIO solo al pedir voz.
- [ ] Cámara solo al usar foto.
- [ ] Exact alarms si aplica.

## R. Reinicio del celular

- [ ] Reiniciar dispositivo.
- [ ] Confirmar reprogramación por `BOOT_COMPLETED`.
- [ ] Confirmar que no se duplican alarmas.

## S. Xiaomi / MIUI batería

- [ ] Activar ahorro agresivo.
- [ ] Repetir prueba de recordatorio hablado.
- [ ] Abrir ayuda de batería desde Ajustes.
- [ ] Documentar comportamiento real.

## T. Release build

- [ ] Instalar APK release.
- [ ] Abrir sin logs de debug visibles.
- [ ] Confirmar notificaciones, alarmas y TTS.
- [ ] Confirmar que PDF, backup y sync siguen funcionando.
