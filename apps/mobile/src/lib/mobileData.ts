import {
  buildOfflineDailyStatus,
  getPersonalizedStatus,
  getPressureStatus,
} from "@/lib/offlineDailyStatus";
import {
  initializeLocalDb,
  resetLocalDbInitialization,
  restoreLocalDbForRecovery,
} from "@/lib/localDb";
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
  ensureDemoPatientExists,
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

function logMobileData(message: string, ...args: unknown[]) {
  if (__DEV__) {
    console.log(`[mobileData] ${message}`, ...args);
  }
}

function assertPositiveRange(
  label: string,
  min: number | null | undefined,
  max: number | null | undefined
) {
  if (min === null || min === undefined || max === null || max === undefined) {
    return;
  }

  if (min <= 0 || max <= 0 || min >= max) {
    throw new Error(`invalid_${label}_range`);
  }
}

function assertPositiveValue(label: string, value: number | null | undefined) {
  if (value === null || value === undefined) {
    return;
  }

  if (value <= 0) {
    throw new Error(`invalid_${label}_value`);
  }
}

export async function initializeMobileData() {
  initializationPromise ??= (async () => {
    logMobileData("Initialization start");
    await initializeLocalDb();
    await seedLocalDataIfNeeded();
    logMobileData("Initialization complete");
  })().catch((error) => {
    console.error("[mobileData] Initialization failed", error);
    initializationPromise = null;
    throw error;
  });

  return initializationPromise;
}

export function resetMobileDataInitialization() {
  initializationPromise = null;
  resetLocalDbInitialization();
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

  const systolic = Number(input.systolic);
  const diastolic = Number(input.diastolic);
  const pulse =
    input.pulse === null || input.pulse === undefined ? null : Number(input.pulse);

  if (
    !Number.isFinite(systolic) ||
    !Number.isFinite(diastolic) ||
    systolic < 50 ||
    systolic > 250 ||
    diastolic < 30 ||
    diastolic > 160 ||
    (pulse !== null &&
      (!Number.isFinite(pulse) || pulse < 30 || pulse > 220))
  ) {
    const error = new Error("invalid_pressure_values");
    if (__DEV__) {
      console.error("[mobileData] Pressure validation failed", input);
    }
    throw error;
  }

  await ensureDemoPatientExists();
  const settings = await getLocalHealthSettings();
  const status = getPressureStatus(systolic, diastolic);
  const personalizedStatus = getPersonalizedStatus(
    {
      systolic,
      diastolic,
      pulse,
    },
    settings
  );

  try {
    return await createLocalBloodPressureReading({
      patientId: input.patientId ?? LOCAL_PATIENT_ID,
      systolic,
      diastolic,
      ...(pulse !== null ? { pulse } : {}),
      notes: input.notes,
      status,
      personalizedStatus,
    });
  } catch (error) {
    console.error("[mobileData] Pressure save failed", error);
    throw error;
  }
}

export async function getHealthSettings(): Promise<PatientHealthSettings | null> {
  await ensureReady();
  return getLocalHealthSettings();
}

export async function updateHealthSettings(input: HealthSettingsInput) {
  await ensureReady();
  try {
    assertPositiveValue("systolicMinNormal", input.systolicMinNormal);
    assertPositiveValue("systolicMaxNormal", input.systolicMaxNormal);
    assertPositiveValue("diastolicMinNormal", input.diastolicMinNormal);
    assertPositiveValue("diastolicMaxNormal", input.diastolicMaxNormal);
    assertPositiveValue("pulseMinNormal", input.pulseMinNormal);
    assertPositiveValue("pulseMaxNormal", input.pulseMaxNormal);
    assertPositiveRange(
      "systolic",
      input.systolicMinNormal,
      input.systolicMaxNormal
    );
    assertPositiveRange(
      "diastolic",
      input.diastolicMinNormal,
      input.diastolicMaxNormal
    );
    assertPositiveRange("pulse", input.pulseMinNormal, input.pulseMaxNormal);
    await ensureDemoPatientExists();
    return await updateLocalHealthSettings(input);
  } catch (error) {
    console.error("[mobileData] Health settings save failed", error);
    throw error;
  }
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

export async function recoverLocalData() {
  resetMobileDataInitialization();
  await restoreLocalDbForRecovery();
  await initializeMobileData();
  await refreshNotificationsIfNeeded();
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
