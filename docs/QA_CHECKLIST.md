# QA Checklist Contigo

## Instalacion
- [ ] Instalar el APK en un celular real.
- [ ] Abrir la app por primera vez sin cierres inesperados.
- [ ] Verificar permisos de notificaciones, microfono y camara segun el flujo.

## Datos
- [ ] Confirmar que el seed inicial aparece una sola vez.
- [ ] Crear un medicamento sin imagen.
- [ ] Crear un medicamento con imagen.
- [ ] Editar un medicamento.
- [ ] Eliminar un medicamento y revisar que no quede imagen huerfana visible.
- [ ] Registrar una presion manual.
- [ ] Editar rangos medicos.

## Recordatorios
- [ ] Activar recordatorios.
- [ ] Recibir recordatorio agrupado por horario.
- [ ] Confirmar vibracion y sonido.
- [ ] Verificar que repite como maximo 3 veces.
- [ ] Marcar tomado y confirmar cancelacion del recordatorio.
- [ ] Reiniciar el celular y validar reprogramacion.

## Voz
- [ ] Probar lectura TTS desde la app.
- [ ] Registrar presion por voz.
- [ ] Confirmar pastilla por voz.
- [ ] Pedir ayuda por voz.
- [ ] Verificar mensaje amigable si el reconocimiento falla.

## Camara
- [ ] Tomar foto de medicamento.
- [ ] Elegir imagen desde galeria.
- [ ] Cancelar camara o galeria sin crash.
- [ ] Quitar imagen y confirmar que el medicamento sigue usable.

## Backup
- [ ] Exportar respaldo ZIP.
- [ ] Confirmar advertencia de privacidad antes de exportar.
- [ ] Importar ZIP valido.
- [ ] Probar fusionar datos.
- [ ] Probar reemplazar datos con doble confirmacion.
- [ ] Importar un ZIP invalido y validar mensaje amigable.

## Reportes
- [ ] Abrir Reportes con ultimos 7 dias.
- [ ] Cambiar a ultimos 30 dias.
- [ ] Cambiar a todo.
- [ ] Generar PDF con datos.
- [ ] Generar PDF sin datos suficientes y validar mensaje basico.
- [ ] Guardar PDF con Storage Access Framework.
- [ ] Compartir PDF con Android Sharesheet.

## Accesibilidad
- [ ] Verificar texto grande en pantallas principales.
- [ ] Verificar botones grandes y faciles de tocar.
- [ ] Confirmar scroll en pantallas largas.
- [ ] Confirmar que el teclado no tape acciones principales.
- [ ] Probar sin internet.

## Privacidad
- [ ] Verificar advertencia: "Este reporte contiene informacion de salud. Compartelo solo con personas de confianza."
- [ ] Verificar advertencia: "El respaldo contiene informacion de salud. Guardalo en un lugar seguro."
- [ ] Confirmar que no se muestran datos medicos completos en logs de depuracion.
- [ ] Confirmar que la app no intenta usar internet.
