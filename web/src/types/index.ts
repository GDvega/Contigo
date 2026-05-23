export type MedicationStatus = "taken" | "pending" | "scheduled";
export type PressureStatus =
  | "normal"
  | "elevated"
  | "high"
  | "NORMAL"
  | "ELEVATED"
  | "HIGH"
  | "CRITICAL";

export interface PatientProfile {
  name: string;
  preferredGreeting: string;
  age: number;
}

export interface Medication {
  id: string;
  name: string;
  dose: string;
  time: string;
  color: string;
  shape: string;
  instructions: string;
  status: MedicationStatus;
}

export interface BloodPressureReading {
  id: string;
  date: string;
  time: string;
  systolic: number;
  diastolic: number;
  pulse?: number | null;
  status: PressureStatus;
}

export interface BloodPressurePatient {
  id: string;
  fullName: string;
}

export interface BloodPressureReadingApi {
  id: string;
  systolic: number;
  diastolic: number;
  pulse?: number | null;
  status: PressureStatus;
  notes?: string | null;
  measuredAt: string;
  patient: BloodPressurePatient;
}

export interface ReportPreviewItem {
  id: string;
  label: string;
}
