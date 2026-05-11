export type DailyMedicationStatus = "TAKEN" | "PENDING";

export type DailyRiskLevel = "low" | "medium" | "high";

export type DailyPressureStatus = "NORMAL" | "ELEVATED" | "HIGH" | "CRITICAL";
export type PersonalizedPressureStatus =
  | "within_range"
  | "out_of_range"
  | "not_configured";

export type DailyStatus = {
  patient: {
    id: string;
    fullName: string;
    age: number | null;
    notes: string | null;
  };
  latestPressure: {
    id: string;
    systolic: number;
    diastolic: number;
    pulse: number | null;
    status: DailyPressureStatus;
    personalizedStatus: PersonalizedPressureStatus;
    notes: string | null;
    measuredAt: string;
  } | null;
  medications: {
    id: string;
    name: string;
    dose: string;
    color: string | null;
    shape: string | null;
    instructions: string | null;
    imageUrl: string | null;
    scheduleTime: string;
    statusToday: DailyMedicationStatus;
    takenAt: string | null;
  }[];
  summary: {
    totalMedications: number;
    takenMedications: number;
    pendingMedications: number;
    allMedicationsTaken: boolean;
    hasPressureReadingToday: boolean;
    hasElevatedPressure: boolean;
    hasHighPressure: boolean;
    hasCriticalPressure: boolean;
    hasOutOfRangePressure: boolean;
    riskLevel: DailyRiskLevel;
  };
};
