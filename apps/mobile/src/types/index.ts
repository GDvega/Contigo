export type PressureStatus = "NORMAL" | "ELEVATED" | "HIGH" | "CRITICAL";

export type DailyRiskLevel = "low" | "medium" | "high";

export type DailyMedicationStatus = "TAKEN" | "PENDING";

export type PersonalizedPressureStatus =
  | "within_range"
  | "out_of_range"
  | "not_configured";

export type Patient = {
  id: string;
  fullName: string;
  age: number | null;
  notes: string | null;
};

export type FamilyContact = {
  id: string;
  patientId: string;
  fullName: string;
  phone: string | null;
  relation: string | null;
  createdAt: string;
};

export type PatientHealthSettings = {
  id: string;
  patientId: string;
  systolicMinNormal: number | null;
  systolicMaxNormal: number | null;
  diastolicMinNormal: number | null;
  diastolicMaxNormal: number | null;
  pulseMinNormal: number | null;
  pulseMaxNormal: number | null;
  doctorRecommendation: string | null;
  updatedAt: string;
};

export type DailyStatus = {
  patient: Patient;
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
  imageUri: string | null;
  scheduleTime: string;
  statusToday: DailyMedicationStatus;
  takenAt: string | null;
};

export type MedicationGroup = {
  scheduleTime: string;
  medications: DailyMedication[];
  totalMedications: number;
  pendingMedications: number;
  takenMedications: number;
  isDue: boolean;
  allTaken: boolean;
};

export type Medication = {
  id: string;
  name: string;
  dose: string;
  color?: string | null;
  shape?: string | null;
  instructions?: string | null;
  imageUri?: string | null;
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
  personalizedStatus?: PersonalizedPressureStatus | null;
  notes?: string | null;
  measuredAt: string;
  patient?: {
    id: string;
    fullName: string;
  };
};

export type MedicalReportSummary = {
  generatedAt: string;
  patient: Patient;
  dailyStatus: DailyStatus["summary"];
  bloodPressure: {
    latestReading: BloodPressureReading | null;
    readings: BloodPressureReading[];
  };
  medications: Medication[];
  medicationAdherence: {
    takenLogsCount: number;
  };
  healthSettings?: PatientHealthSettings | null;
};
