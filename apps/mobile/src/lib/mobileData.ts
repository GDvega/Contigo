import {
  buildOfflineDailyStatus,
  getPersonalizedStatus,
  getPressureStatus,
} from "@/lib/offlineDailyStatus";
import { initializeLocalDb } from "@/lib/localDb";
import { deleteMedicationImage } from "@/lib/localMedicationImages";
import {
  type BloodPressureInput,
  type FamilyContactInput,
  type HealthSettingsInput,
  LOCAL_PATIENT_ID,
  type MedicationInput,
  type MedicationUpdateInput,
  clearLocalRecords as clearLocalRepositoryRecords,
  createBloodPressureReading as createLocalBloodPressureReading,
  createMedication as createLocalMedication,
  createMedicationTakenLog,
  deleteMedication as deleteLocalMedication,
  getFamilyContact as getLocalFamilyContact,
  getBloodPressureReadings as getLocalBloodPressureReadings,
  getCurrentPatient,
  getHealthSettings as getLocalHealthSettings,
  getLatestBloodPressureReading,
  getMedications as getLocalMedications,
  resetLocalDemoData as resetLocalDemoRepositoryData,
  getTakenMedicationLogsCount,
  upsertFamilyContact as upsertLocalFamilyContact,
  upsertPatient as upsertLocalPatient,
  updateHealthSettings as updateLocalHealthSettings,
  updateMedication as updateLocalMedication,
} from "@/lib/localRepositories";
import {
  areMedicationRemindersEnabled,
  cancelAllMedicationReminderNotifications,
  cancelMedicationGroupReminderNotifications,
  cancelMedicationReminderNotifications,
  disableMedicationRemindersFlag,
  scheduleMedicationReminders,
} from "@/lib/notifications";
import { seedLocalDataIfNeeded, seedLocalDemoData } from "@/lib/localSeed";
import type {
  BloodPressureReading,
  FamilyContact,
  MedicalReportSummary,
  MedicationGroup,
  Medication,
  Patient,
  PatientHealthSettings,
} from "@/types";
import {
  getMedicationGroupByScheduleTime,
  getMedicationGroupsForToday as buildMedicationGroupsForToday,
  getMedicationToRegister,
  getNextMedicationGroup as buildNextMedicationGroup,
} from "@/utils/medications";
import { scheduledForTodayIso } from "@/utils/dates";

let initializationPromise: Promise<void> | null = null;

export async function initializeMobileData() {
  initializationPromise ??= (async () => {
    await initializeLocalDb();
    await seedLocalDataIfNeeded();
  })();

  return initializationPromise;
}

async function ensureReady() {
  await initializeMobileData();
}

async function refreshNotificationsIfNeeded() {
  if (!(await areMedicationRemindersEnabled())) {
    return;
  }

  await scheduleMedicationReminders();
}

export async function getDailyStatus() {
  await ensureReady();
  return buildOfflineDailyStatus();
}

export async function getMedicationGroupsForToday(): Promise<MedicationGroup[]> {
  await ensureReady();
  const dailyStatus = await getDailyStatus();
  return buildMedicationGroupsForToday(dailyStatus.medications);
}

export async function getNextMedicationGroup(): Promise<MedicationGroup | null> {
  await ensureReady();
  const dailyStatus = await getDailyStatus();
  return buildNextMedicationGroup(dailyStatus.medications);
}

export async function getPatient(): Promise<Patient | null> {
  await ensureReady();
  return getCurrentPatient();
}

export async function updatePatient(input: {
  fullName: string;
  age?: number | null;
  notes?: string | null;
}) {
  await ensureReady();
  return upsertLocalPatient({
    id: LOCAL_PATIENT_ID,
    fullName: input.fullName,
    age: input.age ?? null,
    notes: input.notes ?? null,
  });
}

export async function getFamilyContact(): Promise<FamilyContact | null> {
  await ensureReady();
  return getLocalFamilyContact();
}

export async function updateFamilyContact(input: FamilyContactInput) {
  await ensureReady();
  return upsertLocalFamilyContact(input);
}

export async function getMedications(): Promise<Medication[]> {
  await ensureReady();
  return getLocalMedications();
}

export async function createMedication(input: MedicationInput) {
  await ensureReady();
  const medication = await createLocalMedication(input);
  await refreshNotificationsIfNeeded();
  return medication;
}

export async function updateMedication(id: string, input: MedicationUpdateInput) {
  await ensureReady();
  const medications = await getLocalMedications();
  const previous = medications.find((medication) => medication.id === id) ?? null;
  const medication = await updateLocalMedication(id, input);

  if (
    previous?.imageUri &&
    previous.imageUri !== medication.imageUri &&
    previous.imageUri.startsWith("file://")
  ) {
    await deleteMedicationImage(previous.imageUri).catch(() => undefined);
  }

  await refreshNotificationsIfNeeded();
  return medication;
}

export async function deleteMedication(id: string) {
  await ensureReady();
  const medications = await getLocalMedications();
  const previous = medications.find((medication) => medication.id === id) ?? null;
  await deleteLocalMedication(id);

  if (previous?.imageUri?.startsWith("file://")) {
    await deleteMedicationImage(previous.imageUri).catch(() => undefined);
  }

  await refreshNotificationsIfNeeded();
}

export async function confirmMedicationTaken() {
  return confirmSingleMedicationTaken();
}

export async function confirmSingleMedicationTaken(medicationId?: string) {
  await ensureReady();

  const dailyStatus = await getDailyStatus();
  const medication = medicationId
    ? dailyStatus.medications.find(
        (item) => item.id === medicationId && item.statusToday === "PENDING"
      ) ?? null
    : getMedicationToRegister(dailyStatus.medications);

  if (!medication) {
    return {
      duplicate: false,
      completed: false,
      message:
        dailyStatus.medications.length === 0
          ? "No hay pastillas configuradas."
          : "Todas tus pastillas de hoy ya fueron registradas.",
    };
  }

  const result = await createMedicationTakenLog(
    medication.id,
    scheduledForTodayIso(medication.scheduleTime)
  );

  if (!result.duplicate) {
    const updatedDailyStatus = await getDailyStatus();
    const groupAfterSave = getMedicationGroupByScheduleTime(
      updatedDailyStatus.medications,
      medication.scheduleTime
    );

    if (!groupAfterSave || groupAfterSave.pendingMedications === 0) {
      await cancelMedicationReminderNotifications(medication.id);
    }
  }

  return {
    duplicate: result.duplicate,
    completed: true,
    medication,
    message: result.duplicate
      ? "Esta toma ya fue registrada."
      : `Listo. Registré que tomaste ${medication.name}.`,
  };
}

export async function confirmMedicationGroupTaken(scheduleTime: string) {
  await ensureReady();

  const dailyStatus = await getDailyStatus();
  const group = getMedicationGroupByScheduleTime(dailyStatus.medications, scheduleTime);

  if (!group || group.pendingMedications === 0) {
    return {
      completed: false,
      duplicate: false,
      message:
        dailyStatus.medications.length === 0
          ? "No hay pastillas configuradas."
          : "Todas tus pastillas de hoy ya fueron registradas.",
      group: null,
      savedCount: 0,
    };
  }

  const pendingMedications = group.medications.filter(
    (medication) => medication.statusToday === "PENDING"
  );
  let savedCount = 0;

  for (const medication of pendingMedications) {
    const result = await createMedicationTakenLog(
      medication.id,
      scheduledForTodayIso(group.scheduleTime)
    );

    if (!result.duplicate) {
      savedCount += 1;
    }
  }

  await cancelMedicationGroupReminderNotifications(
    pendingMedications.map((medication) => medication.id),
    group.scheduleTime
  );

  return {
    completed: true,
    duplicate: savedCount === 0,
    message:
      savedCount === 0
        ? "Estas tomas ya fueron registradas."
        : "Muy bien, registré tus pastillas.",
    group,
    savedCount,
  };
}

export async function getBloodPressureReadings() {
  await ensureReady();
  return getLocalBloodPressureReadings();
}

export async function createBloodPressureReading(
  input: BloodPressureInput
): Promise<BloodPressureReading> {
  await ensureReady();

  const settings = await getLocalHealthSettings();
  const status = getPressureStatus(input.systolic, input.diastolic);
  const personalizedStatus = getPersonalizedStatus(
    {
      systolic: input.systolic,
      diastolic: input.diastolic,
      pulse: input.pulse ?? null,
    },
    settings
  );

  return createLocalBloodPressureReading({
    patientId: input.patientId ?? LOCAL_PATIENT_ID,
    systolic: input.systolic,
    diastolic: input.diastolic,
    pulse: input.pulse,
    notes: input.notes,
    status,
    personalizedStatus,
  });
}

export async function getHealthSettings(): Promise<PatientHealthSettings | null> {
  await ensureReady();
  return getLocalHealthSettings();
}

export async function updateHealthSettings(input: HealthSettingsInput) {
  await ensureReady();
  return updateLocalHealthSettings(input);
}

export async function resetDemoDataLocal() {
  await ensureReady();
  await cancelAllMedicationReminderNotifications().catch(() => undefined);
  await resetLocalDemoRepositoryData();
  await seedLocalDemoData();
  await refreshNotificationsIfNeeded();
}

export async function clearLocalRecords() {
  await ensureReady();
  await cancelAllMedicationReminderNotifications().catch(() => undefined);
  await disableMedicationRemindersFlag().catch(() => undefined);
  await clearLocalRepositoryRecords();
}

export async function getMedicalReportSummary(): Promise<MedicalReportSummary> {
  await ensureReady();

  const [dailyStatus, readings, medications, patient, healthSettings, takenLogsCount] =
    await Promise.all([
      getDailyStatus(),
      getLocalBloodPressureReadings(),
      getLocalMedications(),
      getCurrentPatient(),
      getLocalHealthSettings(),
      getTakenMedicationLogsCount(),
    ]);

  const latestOverall = await getLatestBloodPressureReading();

  return {
    generatedAt: new Date().toISOString(),
    patient: patient ?? dailyStatus.patient,
    dailyStatus: dailyStatus.summary,
    bloodPressure: {
      latestReading: latestOverall
        ? {
            id: latestOverall.id,
            systolic: latestOverall.systolic,
            diastolic: latestOverall.diastolic,
            pulse: latestOverall.pulse,
            status: latestOverall.status,
            notes: latestOverall.notes,
            measuredAt: latestOverall.measuredAt,
          }
        : null,
      readings,
    },
    medications,
    medicationAdherence: {
      takenLogsCount,
    },
    healthSettings,
  };
}

export function getAssetUrl(path?: string | null) {
  if (!path) {
    return null;
  }

  if (
    path.startsWith("http://") ||
    path.startsWith("https://") ||
    path.startsWith("file://") ||
    path.startsWith("content://")
  ) {
    return path;
  }

  return path;
}
