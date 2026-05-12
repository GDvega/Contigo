import * as SQLite from "expo-sqlite";

const DATABASE_NAME = "cuidavoz-offline.db";

let dbPromise: Promise<SQLite.SQLiteDatabase> | null = null;
let initPromise: Promise<void> | null = null;

const schemaSql = `
PRAGMA journal_mode = WAL;

CREATE TABLE IF NOT EXISTS patient (
  id TEXT PRIMARY KEY,
  fullName TEXT,
  age INTEGER,
  notes TEXT,
  createdAt TEXT,
  updatedAt TEXT
);

CREATE TABLE IF NOT EXISTS family_contacts (
  id TEXT PRIMARY KEY,
  fullName TEXT,
  phone TEXT,
  relation TEXT,
  patientId TEXT NOT NULL,
  createdAt TEXT
);

CREATE TABLE IF NOT EXISTS medications (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  dose TEXT NOT NULL,
  color TEXT,
  shape TEXT,
  instructions TEXT,
  imageUri TEXT,
  isActive INTEGER DEFAULT 1,
  patientId TEXT,
  createdAt TEXT,
  updatedAt TEXT
);

CREATE TABLE IF NOT EXISTS medication_schedules (
  id TEXT PRIMARY KEY,
  medicationId TEXT NOT NULL,
  time TEXT NOT NULL,
  isActive INTEGER DEFAULT 1,
  createdAt TEXT
);

CREATE TABLE IF NOT EXISTS medication_logs (
  id TEXT PRIMARY KEY,
  medicationId TEXT NOT NULL,
  status TEXT NOT NULL,
  scheduledFor TEXT,
  takenAt TEXT,
  createdAt TEXT
);

CREATE TABLE IF NOT EXISTS blood_pressure_readings (
  id TEXT PRIMARY KEY,
  patientId TEXT NOT NULL,
  systolic INTEGER NOT NULL,
  diastolic INTEGER NOT NULL,
  pulse INTEGER NULL,
  status TEXT NOT NULL,
  personalizedStatus TEXT NULL,
  notes TEXT NULL,
  measuredAt TEXT NOT NULL,
  createdAt TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS patient_health_settings (
  id TEXT PRIMARY KEY,
  patientId TEXT NOT NULL,
  systolicMinNormal INTEGER,
  systolicMaxNormal INTEGER,
  diastolicMinNormal INTEGER,
  diastolicMaxNormal INTEGER,
  pulseMinNormal INTEGER,
  pulseMaxNormal INTEGER,
  doctorRecommendation TEXT,
  updatedAt TEXT
);

CREATE TABLE IF NOT EXISTS notification_schedules (
  id TEXT PRIMARY KEY,
  medicationId TEXT NOT NULL,
  scheduledFor TEXT NOT NULL,
  notificationId TEXT NOT NULL,
  type TEXT NOT NULL,
  createdAt TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_medications_patient_active
  ON medications (patientId, isActive);
CREATE INDEX IF NOT EXISTS idx_family_contacts_patient
  ON family_contacts (patientId);
CREATE INDEX IF NOT EXISTS idx_medication_schedules_medication_active
  ON medication_schedules (medicationId, isActive);
CREATE INDEX IF NOT EXISTS idx_medication_logs_medication_created
  ON medication_logs (medicationId, createdAt);
CREATE INDEX IF NOT EXISTS idx_blood_pressure_patient_measured
  ON blood_pressure_readings (patientId, measuredAt);
CREATE INDEX IF NOT EXISTS idx_patient_health_settings_patient
  ON patient_health_settings (patientId);
CREATE INDEX IF NOT EXISTS idx_notification_schedules_medication_scheduled
  ON notification_schedules (medicationId, scheduledFor);
CREATE INDEX IF NOT EXISTS idx_notification_schedules_notification
  ON notification_schedules (notificationId);
`;

export async function getLocalDb() {
  dbPromise ??= SQLite.openDatabaseAsync(DATABASE_NAME);
  return dbPromise;
}

export async function initializeLocalDb() {
  initPromise ??= (async () => {
    const db = await getLocalDb();
    await db.execAsync(schemaSql);
    const medicationColumns = await db.getAllAsync<{ name: string }>(
      "PRAGMA table_info(medications)"
    );
    const medicationColumnNames = medicationColumns.map((column) => column.name);

    if (!medicationColumnNames.includes("imageUri")) {
      await db.execAsync("ALTER TABLE medications ADD COLUMN imageUri TEXT");
    }

    if (medicationColumnNames.includes("imageUrl")) {
      await db.execAsync(
        "UPDATE medications SET imageUri = COALESCE(imageUri, imageUrl)"
      );
    }
  })();

  return initPromise;
}
