import { getDailyStatus } from "@/features/daily-status/daily-status.service";
import type { DailyStatus } from "@/features/daily-status/daily-status.types";
import { prisma } from "@/lib/prisma";

const PATIENT_ID = "patient_maria";

export type ReportPressureStatus = "NORMAL" | "ELEVATED" | "HIGH" | "CRITICAL";

export type MedicalReportData = {
  generatedAt: string;
  patient: {
    id: string;
    fullName: string;
    age: number | null;
    notes: string | null;
  };
  healthSettings: {
    systolicMinNormal: number | null;
    systolicMaxNormal: number | null;
    diastolicMinNormal: number | null;
    diastolicMaxNormal: number | null;
    pulseMinNormal: number | null;
    pulseMaxNormal: number | null;
    doctorRecommendation: string | null;
  } | null;
  bloodPressure: {
    latestReading: {
      id: string;
      systolic: number;
      diastolic: number;
      pulse: number | null;
      status: ReportPressureStatus;
      notes: string | null;
      measuredAt: string;
    } | null;
    counts: Record<ReportPressureStatus, number>;
    readings: {
      id: string;
      systolic: number;
      diastolic: number;
      pulse: number | null;
      status: ReportPressureStatus;
      notes: string | null;
      measuredAt: string;
    }[];
  };
  medications: {
    id: string;
    name: string;
    dose: string;
    color: string | null;
    shape: string | null;
    instructions: string | null;
    schedules: {
      id: string;
      time: string;
      isActive: boolean;
    }[];
  }[];
  medicationAdherence: {
    takenLogsCount: number;
    latestLogs: {
      id: string;
      status: "TAKEN" | "MISSED" | "PENDING";
      scheduledFor: string;
      takenAt: string | null;
      medication: {
        id: string;
        name: string;
        dose: string;
      };
    }[];
  };
  dailyStatus: DailyStatus["summary"];
};

function createEmptyPressureCounts(): Record<ReportPressureStatus, number> {
  return {
    NORMAL: 0,
    ELEVATED: 0,
    HIGH: 0,
    CRITICAL: 0,
  };
}

export async function getMedicalReportData(): Promise<MedicalReportData> {
  const [
    patient,
    pressureReadings,
    medications,
    medicationLogs,
    dailyStatus,
    healthSettings,
  ] = await Promise.all([
      prisma.patient.findUnique({
        where: {
          id: PATIENT_ID,
        },
      }),
      prisma.bloodPressureReading.findMany({
        where: {
          patientId: PATIENT_ID,
        },
        orderBy: {
          measuredAt: "desc",
        },
        take: 30,
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
            orderBy: {
              createdAt: "asc",
            },
          },
        },
      }),
      prisma.medicationLog.findMany({
        where: {
          medication: {
            patientId: PATIENT_ID,
          },
        },
        orderBy: {
          createdAt: "desc",
        },
        take: 50,
        include: {
          medication: true,
        },
      }),
      getDailyStatus(),
      prisma.patientHealthSettings.findUnique({
        where: {
          patientId: PATIENT_ID,
        },
      }),
    ]);

  if (!patient) {
    throw new Error("Patient not found");
  }

  const counts = pressureReadings.reduce((currentCounts, reading) => {
    currentCounts[reading.status] += 1;
    return currentCounts;
  }, createEmptyPressureCounts());

  return {
    generatedAt: new Date().toISOString(),
    patient: {
      id: patient.id,
      fullName: patient.fullName,
      age: patient.age,
      notes: patient.notes,
    },
    healthSettings: healthSettings
      ? {
          systolicMinNormal: healthSettings.systolicMinNormal,
          systolicMaxNormal: healthSettings.systolicMaxNormal,
          diastolicMinNormal: healthSettings.diastolicMinNormal,
          diastolicMaxNormal: healthSettings.diastolicMaxNormal,
          pulseMinNormal: healthSettings.pulseMinNormal,
          pulseMaxNormal: healthSettings.pulseMaxNormal,
          doctorRecommendation: healthSettings.doctorRecommendation,
        }
      : null,
    bloodPressure: {
      latestReading: pressureReadings[0]
        ? {
            id: pressureReadings[0].id,
            systolic: pressureReadings[0].systolic,
            diastolic: pressureReadings[0].diastolic,
            pulse: pressureReadings[0].pulse,
            status: pressureReadings[0].status,
            notes: pressureReadings[0].notes,
            measuredAt: pressureReadings[0].measuredAt.toISOString(),
          }
        : null,
      counts,
      readings: pressureReadings.map((reading) => ({
        id: reading.id,
        systolic: reading.systolic,
        diastolic: reading.diastolic,
        pulse: reading.pulse,
        status: reading.status,
        notes: reading.notes,
        measuredAt: reading.measuredAt.toISOString(),
      })),
    },
    medications: medications.map((medication) => ({
      id: medication.id,
      name: medication.name,
      dose: medication.dose,
      color: medication.color,
      shape: medication.shape,
      instructions: medication.instructions,
      schedules: medication.schedules.map((schedule) => ({
        id: schedule.id,
        time: schedule.time,
        isActive: schedule.isActive,
      })),
    })),
    medicationAdherence: {
      takenLogsCount: medicationLogs.filter((log) => log.status === "TAKEN")
        .length,
      latestLogs: medicationLogs.map((log) => ({
        id: log.id,
        status: log.status,
        scheduledFor: log.scheduledFor.toISOString(),
        takenAt: log.takenAt?.toISOString() ?? null,
        medication: {
          id: log.medication.id,
          name: log.medication.name,
          dose: log.medication.dose,
        },
      })),
    },
    dailyStatus: dailyStatus.summary,
  };
}
