import * as SQLite from "expo-sqlite";

const DATABASE_NAME = "cuidavoz-offline.db";
const DATABASE_SCHEMA_VERSION = 4;

let dbPromise: Promise<SQLite.SQLiteDatabase> | null = null;
let initPromise: Promise<void> | null = null;

const patientTableSql = `
CREATE TABLE IF NOT EXISTS patient (
  id TEXT PRIMARY KEY,
  fullName TEXT,
  age INTEGER,
  notes TEXT,
  createdAt TEXT,
  updatedAt TEXT
);
`;

const familyContactsTableSql = `
CREATE TABLE IF NOT EXISTS family_contacts (
  id TEXT PRIMARY KEY,
  fullName TEXT,
  phone TEXT,
  relation TEXT,
  patientId TEXT NOT NULL,
  createdAt TEXT
);
`;

const medicationsTableSql = `
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
`;

const medicationSchedulesTableSql = `
CREATE TABLE IF NOT EXISTS medication_schedules (
  id TEXT PRIMARY KEY,
  medicationId TEXT NOT NULL,
  time TEXT NOT NULL,
  isActive INTEGER DEFAULT 1,
  createdAt TEXT
);
`;

const medicationLogsTableSql = `
CREATE TABLE IF NOT EXISTS medication_logs (
  id TEXT PRIMARY KEY,
  medicationId TEXT NOT NULL,
  status TEXT NOT NULL,
  scheduledFor TEXT,
  takenAt TEXT,
  createdAt TEXT
);
`;

const pressureReadingsTableSql = `
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
`;

const healthSettingsTableSql = `
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
`;

const reminderSettingsTableSql = `
CREATE TABLE IF NOT EXISTS reminder_settings (
  id TEXT PRIMARY KEY,
  patientId TEXT NOT NULL,
  repeatEveryMinutes INTEGER,
  repeatCount INTEGER,
  speakOnOpen INTEGER DEFAULT 1,
  updatedAt TEXT
);
`;

const notificationSchedulesTableSql = `
CREATE TABLE IF NOT EXISTS notification_schedules (
  id TEXT PRIMARY KEY,
  medicationId TEXT NOT NULL,
  scheduledFor TEXT NOT NULL,
  notificationId TEXT NOT NULL,
  type TEXT NOT NULL,
  createdAt TEXT NOT NULL
);
`;

const indexesSql = `
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

function logDebug(message: string, ...args: unknown[]) {
  if (__DEV__) {
    console.log(`[localDb] ${message}`, ...args);
  }
}

export async function getTableInfo(tableName: string) {
  const db = await getLocalDb();
  return db.getAllAsync<{
    cid: number;
    name: string;
    type: string;
    notnull: number;
    dflt_value: string | null;
    pk: number;
  }>(`PRAGMA table_info(${tableName})`);
}

export async function columnExists(tableName: string, columnName: string) {
  const columns = await getTableInfo(tableName);
  return columns.some((column) => column.name === columnName);
}

export async function addColumnIfMissing(tableName: string, columnDefinition: string) {
  const columnName = columnDefinition.trim().split(/\s+/)[0];
  if (await columnExists(tableName, columnName)) {
    return false;
  }

  const db = await getLocalDb();
  await db.execAsync(`ALTER TABLE ${tableName} ADD COLUMN ${columnDefinition}`);
  logDebug(`Added column ${tableName}.${columnName}`);
  return true;
}

async function getUserVersion(db: SQLite.SQLiteDatabase) {
  const row = await db.getFirstAsync<{ user_version: number }>("PRAGMA user_version");
  return row?.user_version ?? 0;
}

async function setUserVersion(db: SQLite.SQLiteDatabase, version: number) {
  await db.execAsync(`PRAGMA user_version = ${version}`);
}

async function createBaseTables(db: SQLite.SQLiteDatabase) {
  await db.execAsync([
    patientTableSql,
    medicationsTableSql,
    medicationSchedulesTableSql,
    medicationLogsTableSql,
    pressureReadingsTableSql,
  ].join("\n"));
}

async function ensureFinalSchema(db: SQLite.SQLiteDatabase) {
  await db.execAsync(
    [
      patientTableSql,
      familyContactsTableSql,
      medicationsTableSql,
      medicationSchedulesTableSql,
      medicationLogsTableSql,
      pressureReadingsTableSql,
      healthSettingsTableSql,
      reminderSettingsTableSql,
      notificationSchedulesTableSql,
      indexesSql,
    ].join("\n")
  );

  await addColumnIfMissing("medications", "imageUri TEXT");
  await addColumnIfMissing("family_contacts", "fullName TEXT");
  await addColumnIfMissing("family_contacts", "phone TEXT");
  await addColumnIfMissing("family_contacts", "relation TEXT");
  await addColumnIfMissing("family_contacts", "patientId TEXT");
  await addColumnIfMissing("family_contacts", "createdAt TEXT");
  await addColumnIfMissing("patient_health_settings", "patientId TEXT");
  await addColumnIfMissing("patient_health_settings", "systolicMinNormal INTEGER");
  await addColumnIfMissing("patient_health_settings", "systolicMaxNormal INTEGER");
  await addColumnIfMissing("patient_health_settings", "diastolicMinNormal INTEGER");
  await addColumnIfMissing("patient_health_settings", "diastolicMaxNormal INTEGER");
  await addColumnIfMissing("patient_health_settings", "pulseMinNormal INTEGER");
  await addColumnIfMissing("patient_health_settings", "pulseMaxNormal INTEGER");
  await addColumnIfMissing("patient_health_settings", "doctorRecommendation TEXT");
  await addColumnIfMissing("patient_health_settings", "updatedAt TEXT");
  await addColumnIfMissing("blood_pressure_readings", "personalizedStatus TEXT");
  await addColumnIfMissing("blood_pressure_readings", "notes TEXT");
  await addColumnIfMissing("notification_schedules", "type TEXT NOT NULL DEFAULT 'medication_reminder'");
  await addColumnIfMissing("notification_schedules", "createdAt TEXT");

  if (await columnExists("medications", "imageUrl")) {
    await db.execAsync(
      "UPDATE medications SET imageUri = COALESCE(imageUri, imageUrl) WHERE imageUrl IS NOT NULL"
    );
  }
}

async function applyMigration(db: SQLite.SQLiteDatabase, version: number) {
  logDebug(`Applying migration v${version}`);

  if (version === 1) {
    await createBaseTables(db);
  } else if (version === 2) {
    await db.execAsync(medicationsTableSql);
    await addColumnIfMissing("medications", "imageUri TEXT");
    if (await columnExists("medications", "imageUrl")) {
      await db.execAsync(
        "UPDATE medications SET imageUri = COALESCE(imageUri, imageUrl) WHERE imageUrl IS NOT NULL"
      );
    }
  } else if (version === 3) {
    await db.execAsync(
      [familyContactsTableSql, healthSettingsTableSql, reminderSettingsTableSql].join("\n")
    );
  } else if (version === 4) {
    await db.execAsync(notificationSchedulesTableSql);
  }

  await setUserVersion(db, version);
}

export async function getLocalDb() {
  dbPromise ??= SQLite.openDatabaseAsync(DATABASE_NAME);
  return dbPromise;
}

export async function initializeLocalDb() {
  initPromise ??= (async () => {
    logDebug("Initialization start");
    const db = await getLocalDb();
    await db.execAsync("PRAGMA journal_mode = WAL;");
    const initialVersion = await getUserVersion(db);
    logDebug(`Current user_version: ${initialVersion}`);

    for (
      let version = Math.max(1, initialVersion + 1);
      version <= DATABASE_SCHEMA_VERSION;
      version += 1
    ) {
      await applyMigration(db, version);
    }

    await ensureFinalSchema(db);

    const finalVersion = await getUserVersion(db);
    logDebug(`Initialization complete at user_version ${finalVersion}`);
  })().catch((error) => {
    console.error("[localDb] Initialization failed", error);
    initPromise = null;
    throw error;
  });

  return initPromise;
}

export async function restoreLocalDbForRecovery() {
  const db = await getLocalDb();
  logDebug("Recovery reset start");
  await db.execAsync(`
    DROP TABLE IF EXISTS notification_schedules;
    DROP TABLE IF EXISTS reminder_settings;
    DROP TABLE IF EXISTS patient_health_settings;
    DROP TABLE IF EXISTS blood_pressure_readings;
    DROP TABLE IF EXISTS medication_logs;
    DROP TABLE IF EXISTS medication_schedules;
    DROP TABLE IF EXISTS medications;
    DROP TABLE IF EXISTS family_contacts;
    DROP TABLE IF EXISTS patient;
    PRAGMA user_version = 0;
  `);
  initPromise = null;
  await initializeLocalDb();
  logDebug("Recovery reset complete");
}

export function resetLocalDbInitialization() {
  initPromise = null;
}
