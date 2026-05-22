import { NativeModules, Platform } from "react-native";

import { speakAsync } from "@/lib/mobileVoice";

type MedicationScheduleGroup = {
  scheduleTime: string;
  medications: Array<{
    id: string;
    name: string;
    dose: string;
  }>;
};

type SpokenReminderSupport = {
  backend: "expo_speech" | "native_android";
  level: "foreground_only" | "background_ready";
  modeLabel: string;
  detail: string;
};

const DEFAULT_FOREGROUND_DELAY_MS = 1100;
const DEFAULT_OPENED_DELAY_MS = 1200;
const DEFAULT_NATIVE_MODULE_NAME = "CuidaVozSpokenReminders";

type NativeSpokenReminderModule = {
  isAvailable?: () => boolean;
  speakReminderNow?: (payload: {
    title: string;
    message: string;
    scheduleTime: string;
    medicationNames: string[];
  }) => Promise<void> | void;
};

function buildMedicationNamesText(names: string[]) {
  if (names.length <= 1) {
    return names[0] ?? "";
  }

  if (names.length === 2) {
    return `${names[0]} y ${names[1]}`;
  }

  return `${names.slice(0, -1).join(", ")} y ${names[names.length - 1]}`;
}

function buildSpokenReminder(group: MedicationScheduleGroup) {
  if (group.medications.length === 1) {
    return `Es hora de tomar ${group.medications[0].name}. Después de tomarla, puedes decir: ya tomé mi pastilla.`;
  }

  return `Es hora de tomar tus pastillas. Debes tomar ${buildMedicationNamesText(
    group.medications.map((medication) => medication.name)
  )}. Después de tomarlas, puedes decir: ya tomé mis pastillas.`;
}

function delay(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function getNativeModule() {
  const module = NativeModules[DEFAULT_NATIVE_MODULE_NAME] as
    | NativeSpokenReminderModule
    | undefined;

  if (!module) {
    return null;
  }

  if (typeof module.isAvailable === "function" && !module.isAvailable()) {
    return null;
  }

  return module;
}

export function getSpokenReminderSupport(): SpokenReminderSupport {
  const nativeModule = Platform.OS === "android" ? getNativeModule() : null;

  if (nativeModule?.speakReminderNow) {
    return {
      backend: "native_android",
      level: "background_ready",
      modeLabel: "Nativo Android",
      detail:
        "La app tiene disponible el backend nativo para avisos hablados incluso fuera de la pantalla principal.",
    };
  }

  return {
    backend: "expo_speech",
    level: "foreground_only",
    modeLabel: "Seguro por fases",
    detail:
      Platform.OS === "android"
        ? "En esta fase la voz funciona con la app abierta o al entrar desde la notificación. El backend nativo Android aún no está activado."
        : "En esta plataforma la voz funciona con la app abierta o al entrar desde la notificación.",
  };
}

async function speakInForeground(
  message: string,
  repeatCount: number,
  pauseMs: number
) {
  const total = Math.max(1, repeatCount);

  for (let index = 0; index < total; index += 1) {
    await speakAsync(message);
    if (index < total - 1) {
      await delay(pauseMs);
    }
  }
}

export async function speakMedicationReminderWithBestAvailableBackend(
  group: MedicationScheduleGroup,
  repeatCount: number
) {
  const support = getSpokenReminderSupport();
  const message = buildSpokenReminder(group);

  if (support.backend === "native_android") {
    const nativeModule = getNativeModule();
    await nativeModule?.speakReminderNow?.({
      title: "Hora de tomar tu medicamento",
      message,
      scheduleTime: group.scheduleTime,
      medicationNames: group.medications.map((medication) => medication.name),
    });
    return;
  }

  await speakInForeground(message, repeatCount, DEFAULT_FOREGROUND_DELAY_MS);
}

export async function speakOpenedMedicationReminderWithBestAvailableBackend(
  group: MedicationScheduleGroup,
  repeatCount: number
) {
  const message = buildSpokenReminder(group);
  await speakInForeground(message, repeatCount, DEFAULT_OPENED_DELAY_MS);
}
