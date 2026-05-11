import { prisma } from "@/lib/prisma";

import type { DailyRiskLevel, DailyStatus } from "./daily-status.types";

const PATIENT_ID = "patient_maria";
export const DAILY_STATUS_DEMO_NOT_FOUND_MESSAGE =
  "Paciente demo no encontrado. Ejecuta npm run db:seed o POST /api/demo/reset.";

export class DailyStatusPatientNotFoundError extends Error {
  constructor() {
    super(DAILY_STATUS_DEMO_NOT_FOUND_MESSAGE);
    this.name = "DailyStatusPatientNotFoundError";
  }
}

function getTodayWindow() {
  const startOfDay = new Date();
  startOfDay.setHours(0, 0, 0, 0);

  const endOfDay = new Date(startOfDay);
  endOfDay.setDate(endOfDay.getDate() + 1);

  return { startOfDay, endOfDay };
}

function isWithinWindow(date: Date, start: Date, end: Date) {
  return date >= start && date < end;
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
  latestPressureStatus?: string;
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

type HealthSettings = {
  systolicMinNormal: number | null;
  systolicMaxNormal: number | null;
  diastolicMinNormal: number | null;
  diastolicMaxNormal: number | null;
  pulseMinNormal: number | null;
  pulseMaxNormal: number | null;
};

function hasConfiguredRange(settings: HealthSettings | null): settings is HealthSettings {
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

function getPersonalizedStatus(
  pressure: {
    systolic: number;
    diastolic: number;
    pulse: number | null;
  } | null,
  settings: HealthSettings | null
) {
  if (!pressure || !hasConfiguredRange(settings)) {
    return "not_configured" as const;
  }

  const isOutOfRange =
    isBelowMin(pressure.systolic, settings.systolicMinNormal) ||
    isAboveMax(pressure.systolic, settings.systolicMaxNormal) ||
    isBelowMin(pressure.diastolic, settings.diastolicMinNormal) ||
    isAboveMax(pressure.diastolic, settings.diastolicMaxNormal) ||
    (pressure.pulse !== null &&
      (isBelowMin(pressure.pulse, settings.pulseMinNormal) ||
        isAboveMax(pressure.pulse, settings.pulseMaxNormal)));

  return isOutOfRange ? ("out_of_range" as const) : ("within_range" as const);
}

export async function getDailyStatus(): Promise<DailyStatus> {
  const { startOfDay, endOfDay } = getTodayWindow();

  const [patient, latestPressure, medications, todayLogs, healthSettings] =
    await Promise.all([
    prisma.patient.findUnique({
      where: {
        id: PATIENT_ID,
      },
    }),
    prisma.bloodPressureReading.findFirst({
      where: {
        patientId: PATIENT_ID,
      },
      orderBy: {
        measuredAt: "desc",
      },
    }),
    prisma.medication.findMany({
      where: {
        patientId: PATIENT_ID,
        isActive: true,
      },
      orderBy: {
        createdAt: "desc",
      },
      include: {
        schedules: {
          where: {
            isActive: true,
          },
          orderBy: {
            createdAt: "asc",
          },
        },
      },
    }),
    prisma.medicationLog.findMany({
      where: {
        status: "TAKEN",
        OR: [
          {
            takenAt: {
              gte: startOfDay,
              lt: endOfDay,
            },
          },
          {
            createdAt: {
              gte: startOfDay,
              lt: endOfDay,
            },
          },
        ],
      },
      orderBy: {
        createdAt: "desc",
      },
    }),
    prisma.patientHealthSettings.findUnique({
      where: {
        patientId: PATIENT_ID,
      },
    }),
  ]);

  if (!patient) {
    throw new DailyStatusPatientNotFoundError();
  }

  const logByMedicationId = new Map(
    todayLogs.map((log) => [log.medicationId, log])
  );

  const dailyMedications = medications
    .map((medication) => {
      const schedule = medication.schedules[0];

      if (!schedule) {
        return null;
      }

      const log = logByMedicationId.get(medication.id);
      const takenAt = log?.takenAt ?? log?.createdAt ?? null;

      return {
        id: medication.id,
        name: medication.name,
        dose: medication.dose,
        color: medication.color,
        shape: medication.shape,
        instructions: medication.instructions,
        imageUrl: medication.imageUrl,
        scheduleTime: schedule.time,
        statusToday: log ? ("TAKEN" as const) : ("PENDING" as const),
        takenAt: takenAt?.toISOString() ?? null,
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
  const allMedicationsTaken = totalMedications > 0 && pendingMedications === 0;
  const hasPressureReadingToday = latestPressure
    ? isWithinWindow(latestPressure.measuredAt, startOfDay, endOfDay)
    : false;
  const hasElevatedPressure = latestPressure?.status === "ELEVATED";
  const hasCriticalPressure = latestPressure?.status === "CRITICAL";
  const hasHighPressure =
    latestPressure?.status === "HIGH" || latestPressure?.status === "CRITICAL";
  const personalizedStatus = getPersonalizedStatus(
    latestPressure
      ? {
          systolic: latestPressure.systolic,
          diastolic: latestPressure.diastolic,
          pulse: latestPressure.pulse,
        }
      : null,
    healthSettings
  );
  const hasOutOfRangePressure = personalizedStatus === "out_of_range";

  return {
    patient: {
      id: patient.id,
      fullName: patient.fullName,
      age: patient.age,
      notes: patient.notes,
    },
    latestPressure: latestPressure
      ? {
          id: latestPressure.id,
          systolic: latestPressure.systolic,
          diastolic: latestPressure.diastolic,
          pulse: latestPressure.pulse,
          status: latestPressure.status,
          personalizedStatus,
          notes: latestPressure.notes,
          measuredAt: latestPressure.measuredAt.toISOString(),
        }
      : null,
    medications: dailyMedications,
    summary: {
      totalMedications,
      takenMedications,
      pendingMedications,
      allMedicationsTaken,
      hasPressureReadingToday,
      hasElevatedPressure,
      hasHighPressure,
      hasCriticalPressure,
      hasOutOfRangePressure,
      riskLevel: calculateRiskLevel({
        latestPressureStatus: latestPressure?.status,
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
