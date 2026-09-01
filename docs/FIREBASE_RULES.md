# Firebase Rules Canonicas

Las reglas desplegables de Contigo estan en la raiz del repositorio:

- `firestore.rules`
- `storage.rules`

`firebase.json` apunta a esos archivos. Las copias bajo `android/` se mantienen sincronizadas como referencia local para la app Android, pero no son la fuente de despliegue.

## Principios

- Todo acceso remoto requiere `request.auth`.
- Un usuario solo accede a una familia si existe `families/{familyId}/members/{uid}`.
- `linkCodes/{code}` no se puede listar ni actualizar.
- Un codigo de vinculacion solo se puede leer por `get` si el usuario esta autenticado y el codigo no expiro.
- Los documentos clinicos validan campos esperados, tipos basicos y roles.
- Storage solo acepta imagenes de medicamentos en el path esperado, con tamano maximo de 5 MB.

## Estructura Protegida

- `families/{familyId}`
- `families/{familyId}/members/{userId}`
- `families/{familyId}/patients/{patientId}`
- `families/{familyId}/patients/{patientId}/medications/{medicationId}`
- `families/{familyId}/patients/{patientId}/pressureReadings/{readingId}`
- `families/{familyId}/patients/{patientId}/medicationLogs/{logId}`
- `families/{familyId}/patients/{patientId}/healthSettings/main`
- `families/{familyId}/patients/{patientId}/alerts/{alertId}`
- `families/{familyId}/patients/{patientId}/contact/main`
- `families/{familyId}/patients/{patientId}/preferences/reminders`
- `linkCodes/{code}`

## Contratos Principales

- La familia solo se crea por un usuario autenticado cuyo `uid` coincide con `createdBy`.
- El miembro `patient` solo se crea para el propio `uid` y si la familia fue creada por ese usuario.
- El miembro `caregiver` solo se crea para el propio `uid` si presenta un `linkCode` valido, no expirado y asociado a la misma familia.
- Los miembros solo actualizan sus campos no sensibles (`displayName`, `phone`, `fcmToken`); el rol no es mutable por update.
- Los medicamentos validan `scheduleType`, fechas Firestore `timestamp`, listas de dias/fechas, campos de imagen y metadatos de sync.
- Lecturas de presion, logs de medicacion, rangos, contacto, preferencias y alertas validan los campos que escribe la app Android.
- Las alertas solo permiten actualizar `seen` despues de creadas.
- Las preferencias solo se escriben en `preferences/reminders`.

## Link Codes

El flujo actual usa auth anonimo y un codigo temporal como secreto compartido:

1. El paciente crea familia, miembro `patient`, snapshot remoto y `linkCodes/{code}`.
2. El cuidador autenticado lee el codigo exacto por `get`.
3. En una transaccion crea su miembro `caregiver` y borra el codigo.

Reglas importantes:

- `allow list: if false`
- `allow update: if false`
- `allow create` exige que `createdBy == request.auth.uid` y que el creador ya sea `patient` de la familia.
- `allow delete` permite al creador borrar su codigo o al cuidador consumirlo dentro de la misma transaccion que crea su membresia.

## Storage

Path admitido:

```text
families/{familyId}/patients/{patientId}/medications/{medicationId}.jpg
```

Reglas:

- `read`: solo miembros de la familia.
- `create/update`: solo miembros, `contentType` `image/*`, maximo 5 MB y nombre `*.jpg` seguro.
- `delete`: solo miembros.
- Cualquier otro path queda denegado.

## Validacion

Las pruebas actuales con Firebase Emulator Suite validan:

- Miembro de familia puede leer sus datos.
- Usuario autenticado de otra familia no puede leer ni escribir datos ajenos.
- `linkCodes` no se puede listar.
- Un cuidador puede consumir un codigo valido en transaccion.
- Un cuidador no puede crear su membresia sin consumir el codigo en la misma transaccion.

Pendiente para ampliar cobertura: codigo expirado y reglas de Storage.
