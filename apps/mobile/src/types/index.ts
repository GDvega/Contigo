export type PressureStatus = "NORMAL" | "ELEVATED" | "HIGH" | "CRITICAL";

export type DailyRiskLevel = "low" | "medium" | "high";

export type DailyMedicationStatus = "TAKEN" | "PENDING";

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
    status: PressureStatus;
    personalizedStatus: PersonalizedPressureStatus;
    notes: string | null;
    measuredAt: string;
  } | null;
  medications: DailyMedication[];
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

export type DailyMedication = {
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
};

export type Medication = {
  id: string;
  name: string;
  dose: string;
  color?: string | null;
  shape?: string | null;
  instructions?: string | null;
  imageUrl?: string | null;
  isActive?: boolean;
  schedules: {
    id: string;
    time: string;
    isActive: boolean;
  }[];
  patient?: {
    id: string;
    fullName: string;
  };
};

export type BloodPressureReading = {
  id: string;
  systolic: number;
  diastolic: number;
  pulse?: number | null;
  status: PressureStatus;
  notes?: string | null;
  measuredAt: string;
  patient?: {
    id: string;
    fullName: string;
  };
};

export type MedicalReportSummary = {
  generatedAt: string;
  patient: DailyStatus["patient"];
  dailyStatus: DailyStatus["summary"];
  bloodPressure: {
    latestReading: BloodPressureReading | null;
    readings: BloodPressureReading[];
  };
  medications: Medication[];
  medicationAdherence: {
    takenLogsCount: number;
  };
};
