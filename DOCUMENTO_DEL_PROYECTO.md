# Contigo — Guía del Proyecto

> Un compañero digital para adultos mayores y sus familias

---

## 1. ¿Qué es Contigo?

**Contigo** es una aplicación móvil de salud domiciliaria que ayuda a las personas mayores (adultos mayores) a gestionar su salud diaria de forma sencilla y segura. Su nombre representa la idea de compañía y cercanía: estar **"contigo"** en el cuidado diario.

La app permite:
- **Recordar los medicamentos** que deben tomarse cada día.
- **Registrar la presión arterial** para llevar control.
- **Pedir ayuda** a un familiar en caso de emergencia.
- **Conectar con un familiar o cuidador** para que pueda seguir la salud del paciente a distancia.

La app está disponible solo para **teléfonos Android** (sistema operativo de Google).

---

## 2. ¿Cuál es el objetivo?

El objetivo principal de Contigo es **mejorar la calidad de vida de los adultos mayores que viven solos o con apoyo familiar limitado**, facilitándoles:

- No olvidarse de tomar sus medicamentos.
- Llevar un registro de su presión arterial.
- Tener un botón de emergencia para contactar a un familiar.
- Permitir que sus cuidadores o familiares estén al tanto de su estado de salud, aunque estén lejos.

> ⚠️ **Nota importante:** Contigo es una herramienta de apoyo. No reemplaza la opinión de un médico ni es un dispositivo médico certificado.

---

## 3. ¿A quién va dirigido?

Contigo tiene tres tipos de usuarios:

### 🧑‍🦳 Paciente (usuario principal)
- Adultos mayores o personas con **poca experiencia digital**.
- Personas que viven solas o con apoyo limitado.
- Aquellos que necesitan recordatorios claros y una interfaz con botones grandes y sencillos.

### 👨‍👩‍👧 Familiar o cuidador (usuario secundario)
- Hijo/a, cónyuge, enfermera o persona de confianza que configura medicamentos, revisa que el paciente cumpla con el tratamiento y genera reportes para el médico.
- Puede estar lejos del paciente y seguir la salud del paciente a través de la app.

### 🩺 Profesional de salud (usuario indirecto)
- Recibe **reportes en formato PDF** generados por el cuidador, con el historial de presión y adherencia al tratamiento.

---

## 4. Funciones principales

La app está organizada en **dos modos** según el rol del usuario que configura el teléfono.

---

### 🏠 Modo Paciente

#### 📋 Pantalla de inicio
- **Saludo personalizado** con el nombre del paciente.
- **Estado del día**: muestra si todo está en orden, si hay medicamentos pendientes o si se debe revisar la presión con un familiar o médico.
- **Tarjeta de presión**: muestra la última presión arterial registrada del día.
- **Tarjeta de medicamento principal**: indica nombre del medicamento, hora e imagen de la pastilla.
- **Botón para medir presión** y **botón de emergencia**.
- **Asistente de voz**: permite interactuar con la app usando comandos hablados.

#### 💊 Recordatorios de medicamentos
- Cuando llega la hora de una pastilla, la app muestra una **notificación con sonido y voz** (el teléfono dice a qué medicina toca).
- El paciente puede responder con un solo toque: **"Ya tomé"** o **"Posponerlo"**.
- Si hay varias pastillas a la misma hora, se agrupan en un solo recordatorio.
- Si el paciente no responde, la app puede reintentar avisar varias veces según la configuración.

#### 🩸 Registro de presión arterial
- Formulario con números grandes para registrar la presión sistólica, diastólica y pulso.
- **Clasificación automática** de la presión: normal, elevada, alta, crítica o fuera de rango personalizado.
- También se puede **registrar presión por voz**: el paciente dice "tengo 120 sobre 80" y la app lo entiende.

#### 🆘 Pedir ayuda (emergencia)
- Muestra el nombre del familiar registrado.
- Botón para **llamar** o **enviar mensaje** (SMS o WhatsApp).
- Acceso rápido desde la pantalla principal.

#### 🎤 Asistente de voz
- Reconoce frases en español coloquial como:
  - "Quiero registrar mi presión"
  - "Ya tomé la pastilla"
  - "Necesito ayuda"
  - "Repite el recordatorio"
- Si no se entiende bien, siempre hay un botón como alternativa.

---

### 👔 Modo Cuidador / Familiar

#### 📊 Panel del cuidador
- **Resumen del paciente**: última presión, estado general y pastillas pendientes del día.
- Estado de sincronización (si los dos teléfonos están conectados a internet).
- Accesos rápidos a todas las funciones de gestión.

#### 💊 Gestión de medicamentos
- Añadir, editar o eliminar medicamentos.
- Configurar la **hora** del recordatorio.
- Tomar una **foto de la pastilla** para que el paciente la reconozca.
- Tipos de tratamiento adaptados: todos los días, días de la semana, fechas específicas, o un número de días.

#### 📈 Registros e histórico
- Ver historial completo de presión arterial.
- Ver logs de medicación (cuándo se tomó cada pastilla, si se pospuso o omitió).

#### 📄 Reporte médico en PDF
- Genera un reporte en PDF con el historial de presión y medicación.
- Períodos disponibles: últimos 7 días, 30 días, o todo el historial.
- Útil para llevar a la consulta médica.

#### 📞 Contacto de emergencia
- Gestiona el nombre, número telefónico y relación del familiar de contacto para emergencias.

#### ⚙️ Ajustes
- **Modo fácil**: interfaz más grande, menos opciones, más ayuda por voz. Ideal para el paciente mayor.
- **Guía por voz**: la app lee las instrucciones al entrar a cada pantalla.
- **Recordatorios**: activar/desactivar, configurar intervalos de repetición y número máximo de avisos.
- **Asistente de voz**: activar o desactivar.
- **Rangos de presión personalizados**: establecer los valores normales del paciente.

#### 💾 Copia de seguridad (backup)
- **Exportar** todos los datos a un archivo ZIP asegura que nadie más pueda acceder a ellos.
- **Importar** desde un ZIP para restaurar los datos en otro teléfono nuevo.
- También disponible durante el primer uso para migrar de un teléfono anterior.

---

## 5. ¿Cómo se conectan el paciente y el cuidador?

Contigo permite que el teléfono del **paciente** y el del **cuidador** estén sincronizados. Funciona así:

1. El paciente genera un **código de vinculación** de 10 caracteres (dura 10 minutos).
2. El cuidador ingresa ese código en su teléfono.
3. Se crea una **"familia" digital** en la nube de Firebase.
4. Los dos teléfonos se sincronizan automáticamente: medicamentos, presiones, registros y alertas.

> 💡 **La app funciona sin internet.** Los cambios se guardan localmente y se sincronizan cuando haya conexión.

---

## 6. Diseño pensado para adultos mayores

Contigo no es una app más. Está diseñada teniendo presente las necesidades de personas mayores con baja alfabetización digital:

| Característica | Detalle |
|---|---|
| **Letras grandes** | Títulos de 30–40 puntos, botones de 24–28 puntos |
| **Botones grandes** | Altura mínima para que sean fáciles de tocar |
| **Alto contraste** | Colores nítidos, texto legible |
| **Pocos botones** | En momentos clave (recordatorio) solo 3 opciones |
| **Voz en todo** | La app puede "hablar" para guiar al paciente |
| **Confirmaciones** | Siempre pide confirmación antes de acciones importantes |
| **Mensajes cálidos** | El tono es cercano, como un familiar: "Hola, María", "Te escucho", "Listo. Toma registrada." |

---

## 7. Tecnologías que usa (a alto nivel)

| Aspecto | Tecnología |
|---|---|
| **Plataforma** | Android (teléfonos con sistema operativo Android) |
| **Lenguaje de programación** | Kotlin |
| **Interfaz visual** | Jetpack Compose (interfaz moderna de Android) |
| **Base de datos local** | Room (almacena datos en el teléfono sin necesidad de internet) |
| **Nube (sincronización)** | Firebase de Google (Firestore, autenticación, notificaciones, almacenamiento de imágenes) |
| **Voz** | Reconocimiento de voz y texto a voz del propio Android |
| **Reportes** | Generación de PDF dentro del teléfono |
| **Almacenamiento de imágenes** | Firebase Storage (fotos de medicamentos en la nube) |
| **Notificaciones** | Servicio nativo de notificaciones de Android |
| **Compilación** | Gradle con Kotlin DSL |

### Dependencias principales del proyecto
- **Jetpack Compose**: para construir la interfaz visual de la app.
- **Hilt**: para gestionar las dependencias del proyecto de forma ordenada.
- **Room**: para guardar datos de manera local en el dispositivo.
- **Firebase**: para sincronización en la nube y notificaciones push.
- **Coil**: para cargar y mostrar imágenes de manera eficiente.

---

## 8. Flujos típicos de uso

### 📱 Día del paciente

1. A la hora de la pastilla, suena el teléfono (notificación con sonido y voz).
2. El paciente abre el aviso y ve la pantalla simple con la foto y el nombre del medicamento.
3. Pulsa **"Ya tomé"** o dice "ya tomé" al micrófono.
4. Por la mañana, registra su presión arterial con el botón grande de la inicio.
5. Si se siente mal, presiona **"Pedir ayuda"** para llamar a su hijo.

### 💻 Día del cuidador

1. Revisa en el panel si hay pastillas pendientes o presión alta del paciente.
2. Añade un nuevo medicamento con foto y horario desde su celular.
3. Genera un reporte PDF de la semana para llevarlo a la consulta médica.
4. Si cambió el celular del paciente, puede guiar la vinculación con un nuevo código o restaurar un respaldo ZIP.

---

## 9. Datos importantes

| Dato | Valor |
|---|---|
| **Nombre comercial** | Contigo |
| **Versión** | 1.0.0 |
| **Plataforma** | Android (teléfonos con Android 8 o superior) |
| **Idioma** | Español (colombiano, latinoamericano y español de España) |
| **Conexión a internet** | No es obligatoria. Funciona offline y sincroniza cuando hay red. |
| **Seguridad** | Los respaldos ZIP están asegurados con contraseña. Los datos personales se almacenan localmente. |
| **Reglas de seguridad de Firebase** | Acceso basado en pertenencia a la familia y rol del usuario, con autenticación anónima. |

---

## 10. Estado actual del proyecto

- La app está en fase de **desarrollo activo** y se busca lanzar la versión 1.0.0 en la Play Store.
- Se ha completado la migración a **Contigo** como app Android nativa, incluyendo el cambio de nombre y la estabilización de funciones como voz, recordatorios y sincronización.
- Se han realizado mejoras de seguridad como **App Check**, cifrado de respaldos ZIP y sistema de códigos de vinculación más robusto.
- La app incluye pruebas unitarias y de integración para garantizar calidad y estabilidad.

---

## 11. Documentos relacionados

- `README.md` — estructura del repositorio y comandos de compilación.
- `ARCHITECTURE.md` — arquitectura técnica detallada de la app Android.
- `PRODUCT_PROMPT.md` — especificación completa del producto (funcionalidades, tono, flujos).
- `FIREBASE_RULES.md` — reglas de seguridad de la base de datos en la nube.
- `AUDIT_REPORT.md` — auditoría técnica del código.
- `QA_ENTERPRISE_AUDIT.md` — checklist de calidad y rendimiento.

---

> **En resumen:** Contigo es una app pensada para que los adultos mayores no se sientan solos ni abrumados con la tecnología, mientras que sus familias pueden estar tranquilas sabiendo que su salud está bajo control.
