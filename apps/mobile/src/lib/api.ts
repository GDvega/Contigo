import { Platform } from "react-native";

import type {
  BloodPressureReading,
  DailyStatus,
  MedicalReportSummary,
  Medication,
} from "@/types";

const MISSING_API_URL_MESSAGE =
  "EXPO_PUBLIC_API_URL no está configurado. Usa la IP de tu computadora, por ejemplo http://192.168.0.103:3000";
const NETWORK_ERROR_MESSAGE =
  "No pudimos conectar con el servidor. Verifica que el backend esté encendido y que EXPO_PUBLIC_API_URL use la IP correcta.";

type ApiEnvelope<T> = {
  data?: T;
  message?: string;
  duplicate?: boolean;
};

export function getApiUrl() {
  const configuredUrl = process.env.EXPO_PUBLIC_API_URL?.trim();

  if (configuredUrl) {
    return configuredUrl.replace(/\/$/, "");
  }

  if (Platform.OS === "web") {
    return "http://localhost:3000";
  }

  throw new Error(MISSING_API_URL_MESSAGE);
}

function buildApiUrl(path: string) {
  return `${getApiUrl()}${path}`;
}

function isNetworkError(error: unknown) {
  return error instanceof TypeError;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  try {
    const response = await fetch(buildApiUrl(path), {
      ...init,
      headers: {
        "Content-Type": "application/json",
        ...init?.headers,
      },
    });
    const payload = (await response.json()) as ApiEnvelope<T>;

    if (!response.ok || payload.data === undefined) {
      throw new Error(payload.message ?? "No se pudo completar la solicitud.");
    }

    return payload.data;
  } catch (error) {
    if (isNetworkError(error)) {
      throw new Error(NETWORK_ERROR_MESSAGE);
    }

    throw error;
  }
}

async function mutation<T>(
  path: string,
  body: Record<string, unknown>
): Promise<ApiEnvelope<T>> {
  try {
    const response = await fetch(buildApiUrl(path), {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    });
    const payload = (await response.json()) as ApiEnvelope<T>;

    if (!response.ok) {
      throw new Error(payload.message ?? "No se pudo completar la solicitud.");
    }

    return payload;
  } catch (error) {
    if (isNetworkError(error)) {
      throw new Error(NETWORK_ERROR_MESSAGE);
    }

    throw error;
  }
}

export function getAssetUrl(path?: string | null) {
  if (!path) {
    return null;
  }

  if (path.startsWith("http")) {
    return path;
  }

  return `${getApiUrl()}${path}`;
}

export const api = {
  getDailyStatus() {
    return request<DailyStatus>("/api/daily-status");
  },

  getMedications() {
    return request<Medication[]>("/api/medications");
  },

  getBloodPressureReadings() {
    return request<BloodPressureReading[]>("/api/blood-pressure");
  },

  getMedicalReportSummary() {
    return request<MedicalReportSummary>("/api/reports/medical-summary");
  },

  createBloodPressure(input: {
    patientId: string;
    systolic: number;
    diastolic: number;
    pulse?: number;
    notes?: string;
  }) {
    return mutation<BloodPressureReading>("/api/blood-pressure", input);
  },

  confirmMedicationIntake(input: {
    medicationId: string;
    scheduledFor: string;
  }) {
    return mutation<unknown>("/api/medication-logs", input);
  },
};
