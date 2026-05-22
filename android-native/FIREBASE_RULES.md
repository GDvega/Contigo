# Firebase Rules Propuestas

Estas reglas son una base propuesta para Firestore en CuidaVoz. No usan acceso abierto.

## Principios

- Todo usuario debe estar autenticado.
- Un usuario solo puede leer o escribir dentro de su `familyId`.
- Un usuario solo accede a una familia si existe en `families/{familyId}/members/{uid}`.
- El paciente puede crear lecturas de presión, logs de tomas y alertas originadas por su propio uso.
- El cuidador puede leer datos del paciente y gestionar medicamentos, rangos y contacto.
- Nadie debe acceder a datos de otra familia.

## Estructura esperada

- `families/{familyId}`
- `families/{familyId}/members/{userId}`
- `families/{familyId}/patients/{patientId}`
- `families/{familyId}/patients/{patientId}/medications/{medicationId}`
- `families/{familyId}/patients/{patientId}/pressureReadings/{readingId}`
- `families/{familyId}/patients/{patientId}/medicationLogs/{logId}`
- `families/{familyId}/patients/{patientId}/healthSettings/main`
- `families/{familyId}/patients/{patientId}/alerts/{alertId}`
- `linkCodes/{code}`

## Reglas sugeridas

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function signedIn() {
      return request.auth != null;
    }

    function memberDoc(familyId) {
      return /databases/$(database)/documents/families/$(familyId)/members/$(request.auth.uid);
    }

    function isFamilyMember(familyId) {
      return signedIn() && exists(memberDoc(familyId));
    }

    function memberRole(familyId) {
      return get(memberDoc(familyId)).data.role;
    }

    function isPatient(familyId) {
      return isFamilyMember(familyId) && memberRole(familyId) == "patient";
    }

    function isCaregiver(familyId) {
      return isFamilyMember(familyId) && memberRole(familyId) == "caregiver";
    }

    match /families/{familyId} {
      allow read: if isFamilyMember(familyId);
      allow create: if signedIn();
      allow update, delete: if isCaregiver(familyId) || isPatient(familyId);

      match /members/{userId} {
        allow read: if isFamilyMember(familyId);
        allow create: if signedIn() && request.auth.uid == userId;
        allow update: if request.auth.uid == userId || isCaregiver(familyId);
        allow delete: if false;
      }

      match /patients/{patientId} {
        allow read: if isFamilyMember(familyId);
        allow create, update: if isPatient(familyId) || isCaregiver(familyId);
        allow delete: if false;

        match /medications/{medicationId} {
          allow read: if isFamilyMember(familyId);
          allow create, update: if isCaregiver(familyId) || isPatient(familyId);
          allow delete: if false;
        }

        match /pressureReadings/{readingId} {
          allow read: if isFamilyMember(familyId);
          allow create: if isPatient(familyId) || isCaregiver(familyId);
          allow update: if false;
          allow delete: if false;
        }

        match /medicationLogs/{logId} {
          allow read: if isFamilyMember(familyId);
          allow create: if isPatient(familyId) || isCaregiver(familyId);
          allow update: if false;
          allow delete: if false;
        }

        match /healthSettings/{docId} {
          allow read: if isFamilyMember(familyId);
          allow create, update: if isCaregiver(familyId) || isPatient(familyId);
          allow delete: if false;
        }

        match /alerts/{alertId} {
          allow read: if isFamilyMember(familyId);
          allow create: if isPatient(familyId) || isCaregiver(familyId);
          allow update: if isFamilyMember(familyId);
          allow delete: if false;
        }

        match /contact/{docId} {
          allow read: if isFamilyMember(familyId);
          allow create, update: if isCaregiver(familyId) || isPatient(familyId);
          allow delete: if false;
        }
      }
    }

    match /linkCodes/{code} {
      allow create: if signedIn();
      allow read: if signedIn();
      allow update: if signedIn();
      allow delete: if false;
    }
  }
}
```

## Validación sugerida para `medications`

Agregar una validación específica dentro de `match /medications/{medicationId}` para permitir y validar:

- `scheduleType`
- `startDate`
- `endDate`
- `daysOfWeek`
- `specificDates`

Ejemplo:

```javascript
match /medications/{medicationId} {
  allow read: if isFamilyMember(familyId);
  allow create, update: if
    (isCaregiver(familyId) || isPatient(familyId)) &&
    request.resource.data.scheduleType in [
      "ALWAYS",
      "DATE_RANGE",
      "WEEKLY_DAYS",
      "SPECIFIC_DATES"
    ] &&
    request.resource.data.startDate is timestamp &&
    (
      !("endDate" in request.resource.data) ||
      request.resource.data.endDate == null ||
      request.resource.data.endDate is timestamp
    ) &&
    request.resource.data.daysOfWeek is list &&
    request.resource.data.specificDates is list;
  allow delete: if false;
}
```

Esto no abre reglas globales nuevas. Solo endurece el esquema esperado del documento de medicamentos.

## Notas de seguridad

- No guardar claves privadas de Firebase Admin SDK en Android.
- No enviar notificaciones FCM directas con secretos desde la app.
- Para avisos al cuidador, usar Cloud Functions o backend futuro con privilegios del servidor.
- Revisar límites de campos permitidos si luego se endurecen reglas por esquema.
