import type {
  BloodPressureReading,
  Medication,
  PatientProfile,
  ReportPreviewItem,
} from "@/types";

export const patientProfile: PatientProfile = {
  name: "María Rojas",
  preferredGreeting: "Buenos días, María",
  age: 72,
};

export const medications: Medication[] = [
  {
    id: "losartan",
    name: "Losartán",
    dose: "50 mg",
    time: "8:00 AM",
    color: "Blanca",
    shape: "Redonda",
    instructions: "1 pastilla blanca, redonda",
    status: "taken",
  },
  {
    id: "aspirin",
    name: "Aspirina",
    dose: "100 mg",
    time: "1:00 PM",
    color: "Amarilla",
    shape: "Ovalada",
    instructions: "1 pastilla amarilla, ovalada",
    status: "pending",
  },
  {
    id: "calcium",
    name: "Calcio + Vitamina D",
    dose: "600 mg",
    time: "8:00 PM",
    color: "Crema",
    shape: "Capsula",
    instructions: "1 cápsula crema",
    status: "scheduled",
  },
];

export const pressureReadings: BloodPressureReading[] = [
  {
    id: "bp-1",
    date: "Hoy",
    time: "8:42 AM",
    systolic: 130,
    diastolic: 85,
    pulse: 72,
    status: "elevated",
  },
  {
    id: "bp-2",
    date: "Hoy",
    time: "7:15 PM",
    systolic: 128,
    diastolic: 82,
    pulse: 70,
    status: "normal",
  },
  {
    id: "bp-3",
    date: "Ayer",
    time: "8:30 AM",
    systolic: 136,
    diastolic: 88,
    pulse: 74,
    status: "elevated",
  },
  {
    id: "bp-4",
    date: "Ayer",
    time: "7:48 PM",
    systolic: 144,
    diastolic: 92,
    pulse: 76,
    status: "high",
  },
  {
    id: "bp-5",
    date: "5 de mayo",
    time: "8:36 AM",
    systolic: 124,
    diastolic: 80,
    pulse: 69,
    status: "normal",
  },
];

export const reportPreviewItems: ReportPreviewItem[] = [
  { id: "1", label: "Promedio semanal de presión arterial" },
  { id: "2", label: "Lecturas agrupadas por día y horario" },
  { id: "3", label: "Cumplimiento de medicamentos" },
  { id: "4", label: "Observaciones para consulta médica" },
];

export const adherenceSummary = {
  weeklyRate: 85,
  takenToday: 2,
  pendingToday: 1,
};

export const reportPeriods = ["Últimos 7 días", "Últimos 15 días", "Último mes"];
