import * as DocumentPicker from "expo-document-picker";
import * as FileSystem from "expo-file-system/legacy";
import * as Sharing from "expo-sharing";

import { getLocalDb, initializeLocalDb } from "@/lib/localDb";
import {
  ensureMedicationImagesDirectory,
  getMedicationImagesDirectory,
} from "@/lib/localMedicationImages";
import {
  areMedicationRemindersEnabled,
  cancelAllMedicationReminderNotifications,
  getReminderSettingsSummary,
  rescheduleAllMedicationReminders,
  setMedicationRemindersEnabled,
} from "@/lib/notifications";

type BackupPatient = {
  id: string;
  fullName: string | null;
  age: number | null;
  notes: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

type BackupFamilyContact = {
  id: string;
  fullName: string | null;
  phone: string | null;
  relation: string | null;
  patientId: string;
  createdAt: string | null;
};

type BackupMedication = {
  id: string;
  name: string;
  dose: string;
  color: string | null;
  shape: string | null;
  instructions: string | null;
  imageUri: string | null;
  isActive: number;
  patientId: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

type BackupMedicationSchedule = {
  id: string;
  medicationId: string;
  time: string;
  isActive: number;
  createdAt: string | null;
};

type BackupMedicationLog = {
  id: string;
  medicationId: string;
  status: string;
  scheduledFor: string | null;
  takenAt: string | null;
  createdAt: string | null;
};

type BackupBloodPressureReading = {
  id: string;
  patientId: string;
  systolic: number;
  diastolic: number;
  pulse: number | null;
  status: string;
  personalizedStatus: string | null;
  notes: string | null;
  measuredAt: string;
  createdAt: string;
};

type BackupHealthSettings = {
  id: string;
  patientId: string;
  systolicMinNormal: number | null;
  systolicMaxNormal: number | null;
  diastolicMinNormal: number | null;
  diastolicMaxNormal: number | null;
  pulseMinNormal: number | null;
  pulseMaxNormal: number | null;
  doctorRecommendation: string | null;
  updatedAt: string | null;
};

type BackupNotificationSchedule = {
  id: string;
  medicationId: string;
  scheduledFor: string;
  notificationId: string;
  type: string;
  createdAt: string;
};

type BackupReminderSetting = {
  enabled: boolean;
  repeatEveryMinutes: number;
  repeatCount: number;
  speakOnOpen: boolean;
};

type BackupMedicationImage = {
  medicationId: string;
  filename: string;
  mimeType: string;
  base64: string;
};

export type CuidaVozBackup = {
  app: "CuidaVoz";
  version: 1;
  exportedAt: string;
  data: {
    patient: BackupPatient[];
    familyContacts: BackupFamilyContact[];
    healthSettings: BackupHealthSettings[];
    medications: BackupMedication[];
    medicationSchedules: BackupMedicationSchedule[];
    medicationLogs: BackupMedicationLog[];
    bloodPressureReadings: BackupBloodPressureReading[];
    reminderSettings: BackupReminderSetting[];
    notificationSchedules?: BackupNotificationSchedule[];
  };
  files: {
    medicationImages: BackupMedicationImage[];
  };
};

const EXPORTS_DIR = `${FileSystem.documentDirectory}exports/`;
const SUPPORTED_BACKUP_VERSION = 1;
const INVALID_BACKUP_MESSAGE =
  "Este archivo no parece ser un respaldo válido de CuidaVoz.";
const IMPORT_FAILED_MESSAGE =
  "No se pudieron importar los datos. El respaldo no fue aplicado.";
const PARTIAL_IMAGE_MESSAGE =
  "Los datos se importaron, pero algunas imágenes no pudieron restaurarse.";

function getMimeTypeFromFilename(filename: string) {
  const extension = filename.split(".").pop()?.toLowerCase();

  if (extension === "png") {
    return "image/png";
  }

  if (extension === "webp") {
    return "image/webp";
  }

  return "image/jpeg";
}

function getExtensionFromMimeType(mimeType: string) {
  if (mimeType === "image/png") {
    return "png";
  }

  if (mimeType === "image/webp") {
    return "webp";
  }

  return "jpg";
}

function createBackupFilename(date = new Date()) {
  return `cuida-voz-backup-${date.toISOString().slice(0, 10)}.json`;
}

async function ensureExportsDirectory() {
  const info = await FileSystem.getInfoAsync(EXPORTS_DIR);

  if (!info.exists) {
    await FileSystem.makeDirectoryAsync(EXPORTS_DIR, {
      intermediates: true,
    });
  }
}

async function loadMedicationImages(
  medications: BackupMedication[]
): Promise<BackupMedicationImage[]> {
  const images: BackupMedicationImage[] = [];

  for (const medication of medications) {
    if (!medication.imageUri?.startsWith("file://")) {
      continue;
    }

    const info = await FileSystem.getInfoAsync(medication.imageUri);

    if (!info.exists) {
      continue;
    }

    const filename =
      medication.imageUri.split("/").pop() ?? `medication-${medication.id}.jpg`;
    const base64 = await FileSystem.readAsStringAsync(medication.imageUri, {
      encoding: FileSystem.EncodingType.Base64,
    });

    images.push({
      medicationId: medication.id,
      filename,
      mimeType: getMimeTypeFromFilename(filename),
      base64,
    });
  }

  return images;
}

async function buildBackup(): Promise<CuidaVozBackup> {
  await initializeLocalDb();
  const db = await getLocalDb();
  const [patient, familyContacts, healthSettings, medications, medicationSchedules, medicationLogs, bloodPressureReadings, notificationSchedules, remindersEnabled] =
    await Promise.all([
      db.getAllAsync<BackupPatient>("SELECT * FROM patient ORDER BY createdAt ASC"),
      db.getAllAsync<BackupFamilyContact>(
        "SELECT * FROM family_contacts ORDER BY createdAt ASC"
      ),
      db.getAllAsync<BackupHealthSettings>(
        "SELECT * FROM patient_health_settings ORDER BY updatedAt ASC"
      ),
      db.getAllAsync<BackupMedication>(
        "SELECT * FROM medications ORDER BY createdAt ASC"
      ),
      db.getAllAsync<BackupMedicationSchedule>(
        "SELECT * FROM medication_schedules ORDER BY createdAt ASC"
      ),
      db.getAllAsync<BackupMedicationLog>(
        "SELECT * FROM medication_logs ORDER BY createdAt ASC"
      ),
      db.getAllAsync<BackupBloodPressureReading>(
        "SELECT * FROM blood_pressure_readings ORDER BY measuredAt ASC"
      ),
      db.getAllAsync<BackupNotificationSchedule>(
        "SELECT * FROM notification_schedules ORDER BY createdAt ASC"
      ),
      areMedicationRemindersEnabled(),
    ]);

  const reminderSettings = getReminderSettingsSummary();
  const medicationImages = await loadMedicationImages(medications);

  return {
    app: "CuidaVoz",
    version: SUPPORTED_BACKUP_VERSION,
    exportedAt: new Date().toISOString(),
    data: {
      patient,
      familyContacts,
      healthSettings,
      medications,
      medicationSchedules,
      medicationLogs,
      bloodPressureReadings,
      reminderSettings: [
        {
          enabled: remindersEnabled,
          repeatEveryMinutes: reminderSettings.repeatEveryMinutes,
          repeatCount: reminderSettings.repeatCount,
          speakOnOpen: reminderSettings.speakOnOpen,
        },
      ],
      notificationSchedules,
    },
    files: {
      medicationImages,
    },
  };
}

function isArrayOfObjects(value: unknown) {
  return Array.isArray(value) && value.every((item) => item && typeof item === "object");
}

export function validateBackup(data: unknown): data is CuidaVozBackup {
  if (!data || typeof data !== "object") {
    return false;
  }

  const candidate = data as Partial<CuidaVozBackup>;

  return (
    candidate.app === "CuidaVoz" &&
    candidate.version === SUPPORTED_BACKUP_VERSION &&
    Boolean(candidate.data) &&
    Boolean(candidate.files) &&
    isArrayOfObjects(candidate.data?.patient) &&
    isArrayOfObjects(candidate.data?.familyContacts) &&
    isArrayOfObjects(candidate.data?.healthSettings) &&
    isArrayOfObjects(candidate.data?.medications) &&
    isArrayOfObjects(candidate.data?.medicationSchedules) &&
    isArrayOfObjects(candidate.data?.medicationLogs) &&
    isArrayOfObjects(candidate.data?.bloodPressureReadings) &&
    isArrayOfObjects(candidate.data?.reminderSettings) &&
    isArrayOfObjects(candidate.files?.medicationImages)
  );
}

async function writeBackupFile(backup: CuidaVozBackup) {
  await ensureExportsDirectory();

  const filename = createBackupFilename();
  const uri = `${EXPORTS_DIR}${filename}`;

  await FileSystem.writeAsStringAsync(uri, JSON.stringify(backup), {
    encoding: FileSystem.EncodingType.UTF8,
  });

  return {
    uri,
    filename,
  };
}

export async function exportCuidaVozBackup(): Promise<{
  uri: string;
  filename: string;
}> {
  const backup = await buildBackup();
  return writeBackupFile(backup);
}

export async function shareBackup(uri: string): Promise<void> {
  const available = await Sharing.isAvailableAsync();

  if (!available) {
    throw new Error("sharing_unavailable");
  }

  await Sharing.shareAsync(uri, {
    mimeType: "application/json",
    dialogTitle: "Compartir respaldo de CuidaVoz",
  });
}

async function restoreMedicationImages(
  backup: CuidaVozBackup
): Promise<{ imageUriByMedicationId: Map<string, string>; failedCount: number }> {
  await ensureMedicationImagesDirectory();
  const directory = getMedicationImagesDirectory();
  const imageUriByMedicationId = new Map<string, string>();
  let failedCount = 0;

  for (const image of backup.files.medicationImages) {
    try {
      const extension = getExtensionFromMimeType(image.mimeType);
      const filename =
        image.filename?.trim() || `medication-${image.medicationId}.${extension}`;
      const destination = `${directory}${filename}`;

      await FileSystem.writeAsStringAsync(destination, image.base64, {
        encoding: FileSystem.EncodingType.Base64,
      });

      imageUriByMedicationId.set(image.medicationId, destination);
    } catch {
      failedCount += 1;
    }
  }

  return { imageUriByMedicationId, failedCount };
}

function normalizeBackupForRestore(
  backup: CuidaVozBackup,
  imageUriByMedicationId: Map<string, string>
) {
  return {
    patient: backup.data.patient,
    familyContacts: backup.data.familyContacts,
    healthSettings: backup.data.healthSettings,
    medications: backup.data.medications.map((medication) => ({
      ...medication,
      imageUri: imageUriByMedicationId.get(medication.id) ?? null,
    })),
    medicationSchedules: backup.data.medicationSchedules,
    medicationLogs: backup.data.medicationLogs,
    bloodPressureReadings: backup.data.bloodPressureReadings,
  };
}

async function replaceDatabaseData(backup: CuidaVozBackup, imageUriByMedicationId: Map<string, string>) {
  const db = await getLocalDb();
  const snapshot = normalizeBackupForRestore(backup, imageUriByMedicationId);

  await db.withTransactionAsync(async () => {
    await db.runAsync("DELETE FROM notification_schedules");
    await db.runAsync("DELETE FROM medication_logs");
    await db.runAsync("DELETE FROM medication_schedules");
    await db.runAsync("DELETE FROM medications");
    await db.runAsync("DELETE FROM blood_pressure_readings");
    await db.runAsync("DELETE FROM patient_health_settings");
    await db.runAsync("DELETE FROM family_contacts");
    await db.runAsync("DELETE FROM patient");

    for (const patient of snapshot.patient) {
      await db.runAsync(
        `INSERT INTO patient (id, fullName, age, notes, createdAt, updatedAt)
         VALUES (?, ?, ?, ?, ?, ?)`,
        patient.id,
        patient.fullName,
        patient.age,
        patient.notes,
        patient.createdAt,
        patient.updatedAt
      );
    }

    for (const contact of snapshot.familyContacts) {
      await db.runAsync(
        `INSERT INTO family_contacts (id, fullName, phone, relation, patientId, createdAt)
         VALUES (?, ?, ?, ?, ?, ?)`,
        contact.id,
        contact.fullName,
        contact.phone,
        contact.relation,
        contact.patientId,
        contact.createdAt
      );
    }

    for (const health of snapshot.healthSettings) {
      await db.runAsync(
        `INSERT INTO patient_health_settings
         (id, patientId, systolicMinNormal, systolicMaxNormal, diastolicMinNormal,
          diastolicMaxNormal, pulseMinNormal, pulseMaxNormal, doctorRecommendation, updatedAt)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        health.id,
        health.patientId,
        health.systolicMinNormal,
        health.systolicMaxNormal,
        health.diastolicMinNormal,
        health.diastolicMaxNormal,
        health.pulseMinNormal,
        health.pulseMaxNormal,
        health.doctorRecommendation,
        health.updatedAt
      );
    }

    for (const medication of snapshot.medications) {
      await db.runAsync(
        `INSERT INTO medications
         (id, name, dose, color, shape, instructions, imageUri, isActive, patientId, createdAt, updatedAt)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        medication.id,
        medication.name,
        medication.dose,
        medication.color,
        medication.shape,
        medication.instructions,
        medication.imageUri,
        medication.isActive,
        medication.patientId,
        medication.createdAt,
        medication.updatedAt
      );
    }

    for (const schedule of snapshot.medicationSchedules) {
      await db.runAsync(
        `INSERT INTO medication_schedules (id, medicationId, time, isActive, createdAt)
         VALUES (?, ?, ?, ?, ?)`,
        schedule.id,
        schedule.medicationId,
        schedule.time,
        schedule.isActive,
        schedule.createdAt
      );
    }

    for (const log of snapshot.medicationLogs) {
      await db.runAsync(
        `INSERT INTO medication_logs (id, medicationId, status, scheduledFor, takenAt, createdAt)
         VALUES (?, ?, ?, ?, ?, ?)`,
        log.id,
        log.medicationId,
        log.status,
        log.scheduledFor,
        log.takenAt,
        log.createdAt
      );
    }

    for (const reading of snapshot.bloodPressureReadings) {
      await db.runAsync(
        `INSERT INTO blood_pressure_readings
         (id, patientId, systolic, diastolic, pulse, status, personalizedStatus, notes, measuredAt, createdAt)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        reading.id,
        reading.patientId,
        reading.systolic,
        reading.diastolic,
        reading.pulse,
        reading.status,
        reading.personalizedStatus,
        reading.notes,
        reading.measuredAt,
        reading.createdAt
      );
    }
  });
}

export async function restoreBackup(backup: CuidaVozBackup): Promise<{ imageWarnings: boolean }> {
  if (!validateBackup(backup)) {
    throw new Error(INVALID_BACKUP_MESSAGE);
  }

  await initializeLocalDb();
  await cancelAllMedicationReminderNotifications().catch(() => undefined);

  const { imageUriByMedicationId, failedCount } = await restoreMedicationImages(backup);
  await replaceDatabaseData(backup, imageUriByMedicationId);

  const remindersEnabled = backup.data.reminderSettings[0]?.enabled === true;
  await setMedicationRemindersEnabled(remindersEnabled);

  if (remindersEnabled) {
    await rescheduleAllMedicationReminders();
  }

  return {
    imageWarnings: failedCount > 0,
  };
}

export async function pickAndImportCuidaVozBackup(): Promise<{
  success: boolean;
  message: string;
}> {
  try {
    const backup = await pickCuidaVozBackup();

    if (backup === null) {
      return {
        success: false,
        message: "cancelled",
      };
    }

    if (backup === "invalid") {
      return {
        success: false,
        message: INVALID_BACKUP_MESSAGE,
      };
    }

    const result = await restoreBackup(backup);

    return {
      success: true,
      message: result.imageWarnings
        ? PARTIAL_IMAGE_MESSAGE
        : "Datos importados correctamente.",
    };
  } catch (error) {
    if (error instanceof SyntaxError) {
      return {
        success: false,
        message: INVALID_BACKUP_MESSAGE,
      };
    }

    return {
      success: false,
      message: IMPORT_FAILED_MESSAGE,
    };
  }
}

export async function pickCuidaVozBackup(): Promise<CuidaVozBackup | null | "invalid"> {
  const selection = await DocumentPicker.getDocumentAsync({
    type: ["application/json", "text/json", "*/*"],
    copyToCacheDirectory: true,
    multiple: false,
  });

  if (selection.canceled || !selection.assets?.[0]?.uri) {
    return null;
  }

  try {
    const content = await FileSystem.readAsStringAsync(selection.assets[0].uri, {
      encoding: FileSystem.EncodingType.UTF8,
    });
    const parsed = JSON.parse(content) as unknown;

    if (!validateBackup(parsed)) {
      return "invalid";
    }

    return parsed;
  } catch {
    return "invalid";
  }
}
