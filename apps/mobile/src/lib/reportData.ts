import { getDailyStatus, getHealthSettings, getMedicalReportSummary } from "@/lib/mobileData";
import {
  getBloodPressureReadings,
  getRecentMedicationLogs,
} from "@/lib/localRepositories";
import type {
  DailyStatus,
  MedicalReportSummary,
  PatientHealthSettings,
} from "@/types";

export type MedicalReportData = {
  generatedAt: string;
  patient: MedicalReportSummary["patient"];
  summary: DailyStatus["summary"];
  latestPressure: DailyStatus["latestPressure"];
  healthSettings: PatientHealthSettings | null;
  bloodPressureHistory: Awaited<ReturnType<typeof getBloodPressureReadings>>;
  medications: MedicalReportSummary["medications"];
  medicationLogs: Awaited<ReturnType<typeof getRecentMedicationLogs>>;
};

export async function getMedicalReportData(): Promise<MedicalReportData> {
  const [dailyStatus, reportSummary, healthSettings, bloodPressureHistory, medicationLogs] =
    await Promise.all([
      getDailyStatus(),
      getMedicalReportSummary(),
      getHealthSettings(),
      getBloodPressureReadings(),
      getRecentMedicationLogs(20),
    ]);

  if (bloodPressureHistory.length === 0 && reportSummary.medications.length === 0) {
    throw new Error("No hay registros suficientes para generar el reporte.");
  }

  return {
    generatedAt: new Date().toISOString(),
    patient: reportSummary.patient,
    summary: dailyStatus.summary,
    latestPressure: dailyStatus.latestPressure,
    healthSettings,
    bloodPressureHistory: bloodPressureHistory.slice(0, 30),
    medications: reportSummary.medications,
    medicationLogs,
  };
}
