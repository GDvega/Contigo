import {
  getCurrentPatient,
  getHealthSettings,
  getLatestBloodPressureReadingForToday,
  getMedicationLogsForToday,
  getMedications,
} from "@/lib/localRepositories";
import type {
  DailyRiskLevel,
  DailyStatus,
  PatientHealthSettings,
  PersonalizedPressureStatus,
  PressureStatus,
} from "@/types";

type HealthSettingsRange = Pick<
  PatientHealthSettings,
  | "systolicMinNormal"
  | "systolicMaxNormal"
  | "diastolicMinNormal"
  | "diastolicMaxNormal"
  | "pulseMinNormal"
  | "pulseMaxNormal"
>;

function getTodayWindow() {
  const startOfDay = new Date();
  startOfDay.setHours(0, 0, 0, 0);

  const endOfDay = new Date(startOfDay);
  endOfDay.setDate(endOfDay.getDate() + 1);

  return {
    startOfDay: startOfDay.toISOString(),
    endOfDay: endOfDay.toISOString(),
  };
}

function getPressureStatus(systolic: number, diastolic: number): PressureStatus {
  if (systolic >= 180 || diastolic >= 120) {
    return "CRITICAL";
  }

  if (systolic >= 140 || diastolic >= 90) {
    return "HIGH";
  }

  if (systolic >= 120 || diastolic >= 80) {
    return "ELEVATED";
  }

  return "NORMAL";
}

function hasConfiguredRange(
  settings: HealthSettingsRange | null
): settings is HealthSettingsRange {
  if (!settings) {
    return false;
  }

  return [
    settings.systolicMinNormal,
    settings.systolicMaxNormal,
    settings.diastolicMinNormal,
    settings.diastolicMaxNormal,
    settings.pulseMinNormal,
    settings.pulseMaxNormal,
  ].some((value) => value !== null);
}

function isBelowMin(value: number, min: number | null) {
  return min !== null && value < min;
}

function isAboveMax(value: number, max: number | null) {
  return max !== null && value > max;
}

export function getPersonalizedStatus(
  pressure: {
    systolic: number;
    diastolic: number;
    pulse: number | null;
  } | null,
  settings: HealthSettingsRange | null
): PersonalizedPressureStatus {
  if (!pressure || !hasConfiguredRange(settings)) {
    return "not_configured";
  }

  const isOutOfRange =
    isBelowMin(pressure.systolic, settings.systolicMinNormal) ||
    isAboveMax(pressure.systolic, settings.systolicMaxNormal) ||
    isBelowMin(pressure.diastolic, settings.diastolicMinNormal) ||
    isAboveMax(pressure.diastolic, settings.diastolicMaxNormal) ||
    (pressure.pulse !== null &&
      (isBelowMin(pressure.pulse, settings.pulseMinNormal) ||
        isAboveMax(pressure.pulse, settings.pulseMaxNormal)));

  return isOutOfRange ? "out_of_range" : "within_range";
}

function calculateRiskLevel({
  latestPressureStatus,
  hasElevatedPressure,
  hasCriticalPressure,
  hasHighPressure,
  hasOutOfRangePressure,
  pendingMedications,
  hasPressureReadingToday,
}: {
  latestPressureStatus?: PressureStatus;
  hasElevatedPressure: boolean;
  hasCriticalPressure: boolean;
  hasHighPressure: boolean;
  hasOutOfRangePressure: boolean;
  pendingMedications: number;
  hasPressureReadingToday: boolean;
}): DailyRiskLevel {
  if (hasCriticalPressure || hasHighPressure) {
    return "high";
  }

  if (
    hasElevatedPressure ||
    hasOutOfRangePressure ||
    pendingMedications > 0 ||
    !hasPressureReadingToday
  ) {
    return "medium";
  }

  if (latestPressureStatus === "NORMAL") {
    return "low";
  }

  return "medium";
}

export async function buildOfflineDailyStatus(): Promise<DailyStatus> {
  const { startOfDay, endOfDay } = getTodayWindow();
  const [patient, medications, todayLogs, latestPressureRow, healthSettings] =
    await Promise.all([
      getCurrentPatient(),
      getMedications(),
      getMedicationLogsForToday(startOfDay, endOfDay),
      getLatestBloodPressureReadingForToday(startOfDay, endOfDay),
      getHealthSettings(),
    ]);

  if (!patient) {
    throw new Error("No hay paciente local configurado.");
  }

  const personalizedStatus = latestPressureRow
    ? getPersonalizedStatus(
        {
          systolic: latestPressureRow.systolic,
          diastolic: latestPressureRow.diastolic,
          pulse: latestPressureRow.pulse,
        },
        healthSettings
      )
    : "not_configured";

  const logByMedicationId = new Map(
    todayLogs.map((log) => [log.medicationId, log] as const)
  );

  const dailyMedications = medications
    .map((medication) => {
      const schedule =
        medication.schedules.find((item) => item.isActive) ?? medication.schedules[0];

      if (!schedule) {
        return null;
      }

      const log = logByMedicationId.get(medication.id);
      const takenAt = log?.takenAt ?? log?.createdAt ?? null;

      return {
        id: medication.id,
        name: medication.name,
        dose: medication.dose,
        color: medication.color ?? null,
        shape: medication.shape ?? null,
        instructions: medication.instructions ?? null,
        imageUri: medication.imageUri ?? null,
        scheduleTime: schedule.time,
        statusToday: log ? ("TAKEN" as const) : ("PENDING" as const),
        takenAt,
      };
    })
    .filter((medication): medication is NonNullable<typeof medication> =>
      Boolean(medication)
    );

  const totalMedications = dailyMedications.length;
  const takenMedications = dailyMedications.filter(
    (medication) => medication.statusToday === "TAKEN"
  ).length;
  const pendingMedications = totalMedications - takenMedications;
  const hasPressureReadingToday = Boolean(latestPressureRow);
  const latestPressureStatus = latestPressureRow
    ? getPressureStatus(latestPressureRow.systolic, latestPressureRow.diastolic)
    : undefined;
  const hasElevatedPressure = latestPressureStatus === "ELEVATED";
  const hasCriticalPressure = latestPressureStatus === "CRITICAL";
  const hasHighPressure =
    latestPressureStatus === "HIGH" || latestPressureStatus === "CRITICAL";
  const hasOutOfRangePressure = personalizedStatus === "out_of_range";

  return {
    patient,
    latestPressure: latestPressureRow
      ? {
          id: latestPressureRow.id,
          systolic: latestPressureRow.systolic,
          diastolic: latestPressureRow.diastolic,
          pulse: latestPressureRow.pulse,
          status: latestPressureStatus ?? latestPressureRow.status,
          personalizedStatus,
          notes: latestPressureRow.notes,
          measuredAt: latestPressureRow.measuredAt,
        }
      : null,
    medications: dailyMedications,
    summary: {
      totalMedications,
      takenMedications,
      pendingMedications,
      allMedicationsTaken: totalMedications > 0 && pendingMedications === 0,
      hasPressureReadingToday,
      hasElevatedPressure,
      hasHighPressure,
      hasCriticalPressure,
      hasOutOfRangePressure,
      riskLevel: calculateRiskLevel({
        latestPressureStatus,
        hasElevatedPressure,
        hasCriticalPressure,
        hasHighPressure,
        hasOutOfRangePressure,
        pendingMedications,
        hasPressureReadingToday,
      }),
    },
  };
}

export { getPressureStatus };
