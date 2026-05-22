import { Platform } from "react-native";
import { requireNativeModule } from "expo-modules-core";

type NativeReminderModule = {
  scheduleMedicationGroupReminder: (
    groupId: string,
    scheduleTimeISO: string,
    medicationNames: string[],
    repeatCount: number
  ) => Promise<{
    scheduled: boolean;
    medicationGroupId: string;
    triggerAtMillis: number;
  }>;
  cancelMedicationGroupReminder: (
    groupId: string
  ) => Promise<{ cancelled: boolean; medicationGroupId: string }>;
  cancelAllMedicationReminders: () => Promise<{ cancelled: boolean }>;
};

let nativeModule: NativeReminderModule | null = null;

function getNativeReminderModule() {
  if (Platform.OS !== "android") {
    return null;
  }

  nativeModule ??= requireNativeModule<NativeReminderModule>(
    "CuidaVozNotificationChannel"
  );

  return nativeModule;
}

export async function scheduleMedicationGroupReminder(
  groupId: string,
  scheduleTimeISO: string,
  medicationNames: string[],
  repeatCount: number
) {
  const module = getNativeReminderModule();
  if (!module) {
    return {
      scheduled: false,
      reason: "android_only",
    } as const;
  }

  return module.scheduleMedicationGroupReminder(
    groupId,
    scheduleTimeISO,
    medicationNames,
    repeatCount
  );
}

export async function cancelMedicationGroupReminder(groupId: string) {
  const module = getNativeReminderModule();
  if (!module) {
    return {
      cancelled: false,
      reason: "android_only",
    } as const;
  }

  return module.cancelMedicationGroupReminder(groupId);
}

export async function cancelAllMedicationReminders() {
  const module = getNativeReminderModule();
  if (!module) {
    return {
      cancelled: false,
      reason: "android_only",
    } as const;
  }

  return module.cancelAllMedicationReminders();
}
