import { getLocalDb } from "@/lib/localDb";
import { createLocalId } from "@/utils/ids";
import type {
  BloodPressureReading,
  FamilyContact,
  Medication,
  Patient,
  PatientHealthSettings,
  PersonalizedPressureStatus,
  PressureStatus,
} from "@/types";

export const LOCAL_PATIENT_ID = "patient_maria";

type MedicationRow = {
  id: string;
  name: string;
  dose: string;
  color: string | null;
  shape: string | null;
  instructions: string | null;
  imageUri: string | null;
  isActive: number;
  patientId: string | null;
  createdAt: string;
  updatedAt: string;
};

type MedicationScheduleRow = {
  id: string;
  medicationId: string;
  time: string;
  isActive: number;
  createdAt: string;
};

type MedicationLogRow = {
  id: string;
  medicationId: string;
  status: string;
  scheduledFor: string | null;
  takenAt: string | null;
  createdAt: string;
};

type MedicationLogWithMedicationRow = MedicationLogRow & {
  medicationName: string;
};

type BloodPressureReadingRow = {
  id: string;
  patientId: string;
  systolic: number;
  diastolic: number;
  pulse: number | null;
  status: PressureStatus;
  personalizedStatus: PersonalizedPressureStatus | null;
  notes: string | null;
  measuredAt: string;
  createdAt: string;
};

type PatientRow = Patient & {
  createdAt: string;
  updatedAt: string;
};

type PatientHealthSettingsRow = PatientHealthSettings;

type FamilyContactRow = FamilyContact;

type NotificationScheduleRow = {
  id: string;
  medicationId: string;
  scheduledFor: string;
  notificationId: string;
  type: string;
  createdAt: string;
};

export type MedicationInput = {
  name: string;
  dose: string;
  time: string;
  color?: string | null;
  shape?: string | null;
  instructions?: string | null;
  imageUri?: string | null;
  patientId?: string;
};

export type MedicationUpdateInput = MedicationInput;

export type BloodPressureInput = {
  patientId?: string;
  systolic: number;
  diastolic: number;
  pulse?: number;
  notes?: string;
};

export type HealthSettingsInput = {
  systolicMinNormal?: number | null;
  systolicMaxNormal?: number | null;
  diastolicMinNormal?: number | null;
  diastolicMaxNormal?: number | null;
  pulseMinNormal?: number | null;
  pulseMaxNormal?: number | null;
  doctorRecommendation?: string | null;
};

export type FamilyContactInput = {
  fullName: string;
  phone?: string | null;
  relation?: string | null;
};

export type MedicationLogHistoryItem = {
  id: string;
  medicationId: string;
  medicationName: string;
  status: string;
  scheduledFor: string | null;
  takenAt: string | null;
  createdAt: string;
};

export type NotificationScheduleInput = {
  medicationId: string;
  scheduledFor: string;
  notificationId: string;
  type: "medication_reminder";
};

function createId(prefix: string) {
  return createLocalId(prefix);
}

function toNullableNumber(value: number | null | undefined) {
  return value ?? null;
}

function placeholders(count: number) {
  return Array.from({ length: count }, () => "?").join(", ");
}

function mapPatientRow(row: PatientRow | null): Patient | null {
  if (!row) {
    return null;
  }

  return {
    id: row.id,
    fullName: row.fullName,
    age: row.age,
    notes: row.notes,
  };
}

function mapHealthSettingsRow(
  row: PatientHealthSettingsRow | null
): PatientHealthSettings | null {
  if (!row) {
    return null;
  }

  return row;
}

function mapFamilyContactRow(row: FamilyContactRow | null): FamilyContact | null {
  if (!row) {
    return null;
  }

  return row;
}

async function getPatientMap(patientIds: string[]) {
  if (patientIds.length === 0) {
    return new Map<string, Patient>();
  }

  const db = await getLocalDb();
  const rows = await db.getAllAsync<PatientRow>(
    `SELECT * FROM patient WHERE id IN (${placeholders(patientIds.length)})`,
    ...patientIds
  );

  return new Map(
    rows
      .map((row) => mapPatientRow(row))
      .filter((row): row is Patient => Boolean(row))
      .map((patient) => [patient.id, patient])
  );
}

async function getSchedulesByMedicationIds(medicationIds: string[]) {
  if (medicationIds.length === 0) {
    return new Map<string, Medication["schedules"]>();
  }

  const db = await getLocalDb();
  const rows = await db.getAllAsync<MedicationScheduleRow>(
    `SELECT * FROM medication_schedules WHERE medicationId IN (${placeholders(
      medicationIds.length
    )}) ORDER BY createdAt ASC`,
    ...medicationIds
  );

  const scheduleMap = new Map<string, Medication["schedules"]>();

  for (const row of rows) {
    const list = scheduleMap.get(row.medicationId) ?? [];
    list.push({
      id: row.id,
      time: row.time,
      isActive: row.isActive === 1,
    });
    scheduleMap.set(row.medicationId, list);
  }

  return scheduleMap;
}

function mapMedicationRow(
  row: MedicationRow,
  patientMap: Map<string, Patient>,
  scheduleMap: Map<string, Medication["schedules"]>
): Medication {
  return {
    id: row.id,
    name: row.name,
    dose: row.dose,
    color: row.color,
    shape: row.shape,
    instructions: row.instructions,
    imageUri: row.imageUri,
    isActive: row.isActive === 1,
    schedules: scheduleMap.get(row.id) ?? [],
    patient: row.patientId ? patientMap.get(row.patientId) : undefined,
  };
}

function mapBloodPressureRow(
  row: BloodPressureReadingRow,
  patient?: Patient
): BloodPressureReading {
  return {
    id: row.id,
    systolic: row.systolic,
    diastolic: row.diastolic,
    pulse: row.pulse,
    status: row.status,
    notes: row.notes,
    measuredAt: row.measuredAt,
    patient,
    ...(row.personalizedStatus ? { personalizedStatus: row.personalizedStatus } : {}),
  } as BloodPressureReading;
}

export async function getPatientCount() {
  const db = await getLocalDb();
  const row = await db.getFirstAsync<{ count: number }>(
    "SELECT COUNT(*) as count FROM patient"
  );

  return row?.count ?? 0;
}

export async function getCurrentPatient() {
  const db = await getLocalDb();
  const row = await db.getFirstAsync<PatientRow>(
    "SELECT * FROM patient WHERE id = ? LIMIT 1",
    LOCAL_PATIENT_ID
  );

  return mapPatientRow(row);
}

export async function ensureDemoPatientExists() {
  const patient = await getCurrentPatient();
  if (patient) {
    return patient;
  }

  return upsertPatient({
    id: LOCAL_PATIENT_ID,
    fullName: "María Rojas",
    age: 72,
    notes: "Paciente de prueba",
  });
}

export async function getMedicationCount() {
  const db = await getLocalDb();
  const row = await db.getFirstAsync<{ count: number }>(
    "SELECT COUNT(*) as count FROM medications WHERE isActive = 1"
  );

  return row?.count ?? 0;
}

export async function upsertPatient(patient: Patient) {
  const db = await getLocalDb();
  const now = new Date().toISOString();
  const existing = await db.getFirstAsync<{ id: string }>(
    "SELECT id FROM patient WHERE id = ? LIMIT 1",
    patient.id
  );

  if (existing) {
    await db.runAsync(
      `UPDATE patient
       SET fullName = ?, age = ?, notes = ?, updatedAt = ?
       WHERE id = ?`,
      patient.fullName,
      toNullableNumber(patient.age),
      patient.notes,
      now,
      patient.id
    );
  } else {
    await db.runAsync(
      `INSERT INTO patient (id, fullName, age, notes, createdAt, updatedAt)
       VALUES (?, ?, ?, ?, ?, ?)`,
      patient.id,
      patient.fullName,
      toNullableNumber(patient.age),
      patient.notes,
      now,
      now
    );
  }

  return (await getCurrentPatient())!;
}

export async function getFamilyContact() {
  const db = await getLocalDb();
  const row = await db.getFirstAsync<FamilyContactRow>(
    `SELECT *
     FROM family_contacts
     WHERE patientId = ?
     ORDER BY createdAt ASC
     LIMIT 1`,
    LOCAL_PATIENT_ID
  );

  return mapFamilyContactRow(row);
}

export async function upsertFamilyContact(input: FamilyContactInput) {
  const db = await getLocalDb();
  const now = new Date().toISOString();
  const existing = await db.getFirstAsync<{ id: string }>(
    "SELECT id FROM family_contacts WHERE patientId = ? LIMIT 1",
    LOCAL_PATIENT_ID
  );

  if (existing) {
    await db.runAsync(
      `UPDATE family_contacts
       SET fullName = ?, phone = ?, relation = ?
       WHERE id = ?`,
      input.fullName,
      input.phone ?? null,
      input.relation ?? null,
      existing.id
    );
  } else {
    await db.runAsync(
      `INSERT INTO family_contacts (id, fullName, phone, relation, patientId, createdAt)
       VALUES (?, ?, ?, ?, ?, ?)`,
      createId("family_contact"),
      input.fullName,
      input.phone ?? null,
      input.relation ?? null,
      LOCAL_PATIENT_ID,
      now
    );
  }

  return (await getFamilyContact())!;
}

export async function getMedications() {
  const db = await getLocalDb();
  const rows = await db.getAllAsync<MedicationRow>(
    "SELECT * FROM medications WHERE isActive = 1 ORDER BY createdAt ASC"
  );
  const medicationIds = rows.map((row) => row.id);
  const patientIds = [...new Set(rows.map((row) => row.patientId).filter(Boolean))] as string[];
  const [patientMap, scheduleMap] = await Promise.all([
    getPatientMap(patientIds),
    getSchedulesByMedicationIds(medicationIds),
  ]);

  return rows.map((row) => mapMedicationRow(row, patientMap, scheduleMap));
}

export async function createMedication(data: MedicationInput) {
  const db = await getLocalDb();
  const now = new Date().toISOString();
  const medicationId = createId("medication");
  const scheduleId = createId("schedule");
  const patientId = data.patientId ?? LOCAL_PATIENT_ID;

  await db.withTransactionAsync(async () => {
    await db.runAsync(
      `INSERT INTO medications
       (id, name, dose, color, shape, instructions, imageUri, isActive, patientId, createdAt, updatedAt)
       VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?)`,
      medicationId,
      data.name,
      data.dose,
      data.color ?? null,
      data.shape ?? null,
      data.instructions ?? null,
      data.imageUri ?? null,
      patientId,
      now,
      now
    );

    await db.runAsync(
      `INSERT INTO medication_schedules (id, medicationId, time, isActive, createdAt)
       VALUES (?, ?, ?, 1, ?)`,
      scheduleId,
      medicationId,
      data.time,
      now
    );
  });

  const medications = await getMedications();
  return medications.find((medication) => medication.id === medicationId)!;
}

export async function updateMedication(id: string, data: MedicationUpdateInput) {
  const db = await getLocalDb();
  const now = new Date().toISOString();
  const existingSchedule = await db.getFirstAsync<{ id: string }>(
    `SELECT id
     FROM medication_schedules
     WHERE medicationId = ? AND isActive = 1
     ORDER BY createdAt ASC
     LIMIT 1`,
    id
  );

  await db.withTransactionAsync(async () => {
    await db.runAsync(
      `UPDATE medications
       SET name = ?, dose = ?, color = ?, shape = ?, instructions = ?, imageUri = ?, updatedAt = ?
       WHERE id = ?`,
      data.name,
      data.dose,
      data.color ?? null,
      data.shape ?? null,
      data.instructions ?? null,
      data.imageUri ?? null,
      now,
      id
    );

    if (existingSchedule) {
      await db.runAsync(
        "UPDATE medication_schedules SET time = ?, isActive = 1 WHERE id = ?",
        data.time,
        existingSchedule.id
      );
    } else {
      await db.runAsync(
        `INSERT INTO medication_schedules (id, medicationId, time, isActive, createdAt)
         VALUES (?, ?, ?, 1, ?)`,
        createId("schedule"),
        id,
        data.time,
        now
      );
    }
  });

  const medications = await getMedications();
  return medications.find((medication) => medication.id === id)!;
}

export async function deleteMedication(id: string) {
  const db = await getLocalDb();

  await db.withTransactionAsync(async () => {
    await db.runAsync("UPDATE medications SET isActive = 0 WHERE id = ?", id);
    await db.runAsync(
      "UPDATE medication_schedules SET isActive = 0 WHERE medicationId = ?",
      id
    );
  });
}

export async function getMedicationLogsForToday(startOfDay: string, endOfDay: string) {
  const db = await getLocalDb();
  return db.getAllAsync<MedicationLogRow>(
    `SELECT * FROM medication_logs
     WHERE status = 'TAKEN'
       AND ((takenAt >= ? AND takenAt < ?) OR (createdAt >= ? AND createdAt < ?))
     ORDER BY createdAt DESC`,
    startOfDay,
    endOfDay,
    startOfDay,
    endOfDay
  );
}

export async function getTakenMedicationLogsCount() {
  const db = await getLocalDb();
  const row = await db.getFirstAsync<{ count: number }>(
    "SELECT COUNT(*) as count FROM medication_logs WHERE status = 'TAKEN'"
  );

  return row?.count ?? 0;
}

export async function getRecentMedicationLogs(limit = 20) {
  const db = await getLocalDb();
  const rows = await db.getAllAsync<MedicationLogWithMedicationRow>(
    `SELECT
       medication_logs.id,
       medication_logs.medicationId,
       medication_logs.status,
       medication_logs.scheduledFor,
       medication_logs.takenAt,
       medication_logs.createdAt,
       medications.name AS medicationName
     FROM medication_logs
     INNER JOIN medications ON medications.id = medication_logs.medicationId
     ORDER BY COALESCE(medication_logs.takenAt, medication_logs.createdAt) DESC
     LIMIT ?`,
    limit
  );

  return rows.map<MedicationLogHistoryItem>((row) => ({
    id: row.id,
    medicationId: row.medicationId,
    medicationName: row.medicationName,
    status: row.status,
    scheduledFor: row.scheduledFor,
    takenAt: row.takenAt,
    createdAt: row.createdAt,
  }));
}

export async function createMedicationTakenLog(
  medicationId: string,
  scheduledFor: string
) {
  const db = await getLocalDb();
  const date = new Date(scheduledFor);
  const startOfDay = new Date(date);
  startOfDay.setHours(0, 0, 0, 0);
  const endOfDay = new Date(startOfDay);
  endOfDay.setDate(endOfDay.getDate() + 1);

  const existing = await db.getFirstAsync<{ id: string }>(
    `SELECT id
     FROM medication_logs
     WHERE medicationId = ?
       AND status = 'TAKEN'
       AND ((takenAt >= ? AND takenAt < ?) OR (createdAt >= ? AND createdAt < ?))
     LIMIT 1`,
    medicationId,
    startOfDay.toISOString(),
    endOfDay.toISOString(),
    startOfDay.toISOString(),
    endOfDay.toISOString()
  );

  if (existing) {
    return { duplicate: true };
  }

  const now = new Date().toISOString();
  await db.runAsync(
    `INSERT INTO medication_logs (id, medicationId, status, scheduledFor, takenAt, createdAt)
     VALUES (?, ?, 'TAKEN', ?, ?, ?)`,
    createId("log"),
    medicationId,
    scheduledFor,
    now,
    now
  );

  return { duplicate: false };
}

export async function createNotificationSchedule(data: NotificationScheduleInput) {
  const db = await getLocalDb();
  const now = new Date().toISOString();

  await db.runAsync(
    `INSERT INTO notification_schedules
     (id, medicationId, scheduledFor, notificationId, type, createdAt)
     VALUES (?, ?, ?, ?, ?, ?)`,
    createId("reminder"),
    data.medicationId,
    data.scheduledFor,
    data.notificationId,
    data.type,
    now
  );
}

export async function getNotificationSchedulesForMedicationDate(
  medicationId: string,
  date: string
) {
  const db = await getLocalDb();
  return db.getAllAsync<NotificationScheduleRow>(
    `SELECT *
     FROM notification_schedules
     WHERE medicationId = ?
       AND type = 'medication_reminder'
       AND substr(scheduledFor, 1, 10) = ?
     ORDER BY scheduledFor ASC`,
    medicationId,
    date
  );
}

export async function getAllNotificationSchedules() {
  const db = await getLocalDb();
  return db.getAllAsync<NotificationScheduleRow>(
    "SELECT * FROM notification_schedules ORDER BY scheduledFor ASC"
  );
}

export async function deleteNotificationSchedulesByIds(notificationIds: string[]) {
  if (notificationIds.length === 0) {
    return;
  }

  const db = await getLocalDb();
  await db.runAsync(
    `DELETE FROM notification_schedules
     WHERE notificationId IN (${placeholders(notificationIds.length)})`,
    ...notificationIds
  );
}

export async function deleteNotificationSchedulesForMedicationDate(
  medicationId: string,
  date: string
) {
  const db = await getLocalDb();
  await db.runAsync(
    `DELETE FROM notification_schedules
     WHERE medicationId = ?
       AND type = 'medication_reminder'
       AND substr(scheduledFor, 1, 10) = ?`,
    medicationId,
    date
  );
}

export async function deleteAllNotificationSchedules() {
  const db = await getLocalDb();
  await db.runAsync("DELETE FROM notification_schedules");
}

export async function resetLocalDemoData() {
  const db = await getLocalDb();

  await db.withTransactionAsync(async () => {
    await db.runAsync("DELETE FROM notification_schedules");
    await db.runAsync("DELETE FROM medication_logs");
    await db.runAsync("DELETE FROM medication_schedules");
    await db.runAsync("DELETE FROM medications");
    await db.runAsync("DELETE FROM blood_pressure_readings");
    await db.runAsync("DELETE FROM patient_health_settings");
  });
}

export async function clearLocalRecords() {
  const db = await getLocalDb();

  await db.withTransactionAsync(async () => {
    await db.runAsync("DELETE FROM notification_schedules");
    await db.runAsync("DELETE FROM medication_logs");
    await db.runAsync("DELETE FROM medication_schedules");
    await db.runAsync("DELETE FROM medications");
    await db.runAsync("DELETE FROM blood_pressure_readings");
    await db.runAsync("DELETE FROM patient_health_settings");
  });
}

export async function getBloodPressureReadings() {
  const db = await getLocalDb();
  const rows = await db.getAllAsync<BloodPressureReadingRow>(
    "SELECT * FROM blood_pressure_readings ORDER BY measuredAt DESC"
  );
  const patient = await getCurrentPatient();

  return rows.map((row) => mapBloodPressureRow(row, patient ?? undefined));
}

export async function getLatestBloodPressureReading() {
  const db = await getLocalDb();
  return db.getFirstAsync<BloodPressureReadingRow>(
    "SELECT * FROM blood_pressure_readings ORDER BY measuredAt DESC LIMIT 1"
  );
}

export async function getLatestBloodPressureReadingForToday(
  startOfDay: string,
  endOfDay: string
) {
  const db = await getLocalDb();
  return db.getFirstAsync<BloodPressureReadingRow>(
    `SELECT * FROM blood_pressure_readings
     WHERE measuredAt >= ? AND measuredAt < ?
     ORDER BY measuredAt DESC
     LIMIT 1`,
    startOfDay,
    endOfDay
  );
}

export async function createBloodPressureReading(
  input: BloodPressureInput & {
    status: PressureStatus;
    personalizedStatus: PersonalizedPressureStatus;
  }
) {
  const db = await getLocalDb();
  const now = new Date().toISOString();
  const patient = await ensureDemoPatientExists();
  const patientId = input.patientId ?? patient.id;
  const id = createId("pressure");

  await db.runAsync(
    `INSERT INTO blood_pressure_readings
     (id, patientId, systolic, diastolic, pulse, status, personalizedStatus, notes, measuredAt, createdAt)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    id,
    patientId,
    input.systolic,
    input.diastolic,
    input.pulse ?? null,
    input.status,
    input.personalizedStatus,
    input.notes ?? null,
    now,
    now
  );

  const row = await db.getFirstAsync<BloodPressureReadingRow>(
    "SELECT * FROM blood_pressure_readings WHERE id = ? LIMIT 1",
    id
  );

  return mapBloodPressureRow(row!, patient);
}

export async function getHealthSettings() {
  const db = await getLocalDb();
  const row = await db.getFirstAsync<PatientHealthSettingsRow>(
    "SELECT * FROM patient_health_settings WHERE patientId = ? LIMIT 1",
    LOCAL_PATIENT_ID
  );

  return mapHealthSettingsRow(row);
}

export async function updateHealthSettings(input: HealthSettingsInput) {
  const db = await getLocalDb();
  const now = new Date().toISOString();
  const patient = await ensureDemoPatientExists();
  const existing = await db.getFirstAsync<{ id: string }>(
    "SELECT id FROM patient_health_settings WHERE patientId = ? LIMIT 1",
    LOCAL_PATIENT_ID
  );

  if (existing) {
    await db.runAsync(
      `UPDATE patient_health_settings
       SET systolicMinNormal = ?, systolicMaxNormal = ?, diastolicMinNormal = ?,
           diastolicMaxNormal = ?, pulseMinNormal = ?, pulseMaxNormal = ?,
           doctorRecommendation = ?, updatedAt = ?
       WHERE id = ?`,
      input.systolicMinNormal ?? null,
      input.systolicMaxNormal ?? null,
      input.diastolicMinNormal ?? null,
      input.diastolicMaxNormal ?? null,
      input.pulseMinNormal ?? null,
      input.pulseMaxNormal ?? null,
      input.doctorRecommendation ?? null,
      now,
      existing.id
    );
  } else {
    await db.runAsync(
      `INSERT INTO patient_health_settings
       (id, patientId, systolicMinNormal, systolicMaxNormal, diastolicMinNormal,
        diastolicMaxNormal, pulseMinNormal, pulseMaxNormal, doctorRecommendation, updatedAt)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      createId("health_settings"),
      patient.id,
      input.systolicMinNormal ?? null,
      input.systolicMaxNormal ?? null,
      input.diastolicMinNormal ?? null,
      input.diastolicMaxNormal ?? null,
      input.pulseMinNormal ?? null,
      input.pulseMaxNormal ?? null,
      input.doctorRecommendation ?? null,
      now
    );
  }

  return (await getHealthSettings())!;
}
