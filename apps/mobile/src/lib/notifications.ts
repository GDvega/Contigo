import AsyncStorage from "@react-native-async-storage/async-storage";
import Constants, { ExecutionEnvironment } from "expo-constants";
import { Platform } from "react-native";

import type { Medication } from "@/types";

const STORAGE_KEY = "cuidavoz.medicationNotificationIds";
const CHANNEL_ID = "medication-reminders";
const EXPO_GO_REASON = "expo_go_not_supported";

type ExpoNotifications = typeof import("expo-notifications");

type NotificationPermissionResult =
  | {
      granted: true;
    }
  | {
      granted: false;
      reason: string;
    };

type NotificationScheduleResult =
  | {
      scheduled: true;
      count: number;
    }
  | {
      scheduled: false;
      reason: string;
    };

type TestNotificationResult =
  | {
      scheduled: true;
      identifier: string;
    }
  | {
      scheduled: false;
      reason: string;
    };

type NotificationCancelResult =
  | {
      cancelled: true;
    }
  | {
      cancelled: false;
      reason: string;
    };

let notificationsModule: ExpoNotifications | null = null;

function isExpoGo() {
  return Constants.executionEnvironment === ExecutionEnvironment.StoreClient;
}

async function getNotifications() {
  if (isExpoGo()) {
    return null;
  }

  try {
    notificationsModule ??= await import("expo-notifications");
    return notificationsModule;
  } catch {
    return null;
  }
}

export async function configureNotificationBehavior() {
  const Notifications = await getNotifications();

  if (!Notifications) {
    return {
      configured: false,
      reason: EXPO_GO_REASON,
    };
  }

  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldPlaySound: true,
      shouldSetBadge: false,
      shouldShowBanner: true,
      shouldShowList: true,
    }),
  });

  return {
    configured: true,
  };
}

export async function requestNotificationPermissions(): Promise<NotificationPermissionResult> {
  const Notifications = await getNotifications();

  if (!Notifications) {
    return {
      granted: false,
      reason: EXPO_GO_REASON,
    };
  }

  await configureNotificationBehavior();

  if (Platform.OS === "android") {
    await Notifications.setNotificationChannelAsync(CHANNEL_ID, {
      name: "Recordatorios de pastillas",
      importance: Notifications.AndroidImportance.HIGH,
      sound: "default",
    });
  }

  const current = await Notifications.getPermissionsAsync();
  if (current.granted) {
    return { granted: true };
  }

  const requested = await Notifications.requestPermissionsAsync({
    ios: {
      allowAlert: true,
      allowBadge: true,
      allowSound: true,
    },
  });

  return requested.granted
    ? { granted: true }
    : { granted: false, reason: "permission_denied" };
}

export async function cancelMedicationNotifications(): Promise<NotificationCancelResult> {
  const Notifications = await getNotifications();

  if (!Notifications) {
    return {
      cancelled: false,
      reason: EXPO_GO_REASON,
    };
  }

  const storedIds = await getStoredNotificationIds();

  await Promise.all(
    storedIds.map((identifier) =>
      Notifications.cancelScheduledNotificationAsync(identifier).catch(() => undefined)
    )
  );

  const scheduled = await Notifications.getAllScheduledNotificationsAsync();
  const medicationReminders = scheduled.filter(
    (notification) =>
      notification.content.data?.type === "medication_reminder"
  );

  await Promise.all(
    medicationReminders.map((notification) =>
      Notifications.cancelScheduledNotificationAsync(notification.identifier).catch(
        () => undefined
      )
    )
  );

  await AsyncStorage.removeItem(STORAGE_KEY);

  return {
    cancelled: true,
  };
}

export async function scheduleMedicationNotifications(
  medications: Medication[]
): Promise<NotificationScheduleResult> {
  const Notifications = await getNotifications();

  if (!Notifications) {
    return {
      scheduled: false,
      reason: EXPO_GO_REASON,
    };
  }

  await cancelMedicationNotifications();

  const scheduledIds: string[] = [];
  const scheduledKeys = new Set<string>();

  for (const medication of medications) {
    if (medication.isActive === false) {
      continue;
    }

    const activeSchedules = medication.schedules.filter(
      (schedule) => schedule.isActive && isValidScheduleTime(schedule.time)
    );

    for (const schedule of activeSchedules) {
      const key = `${medication.id}:${schedule.time}`;

      if (scheduledKeys.has(key)) {
        continue;
      }

      const [hour, minute] = parseScheduleTime(schedule.time);
      const identifier = await Notifications.scheduleNotificationAsync({
        content: {
          title: "Hora de tomar tu pastilla",
          body: `${medication.name} · ${medication.dose} · ${schedule.time}`,
          data: {
            type: "medication_reminder",
            medicationId: medication.id,
          },
          sound: "default",
        },
        trigger: {
          type: Notifications.SchedulableTriggerInputTypes.DAILY,
          channelId: CHANNEL_ID,
          hour,
          minute,
        },
      });

      scheduledIds.push(identifier);
      scheduledKeys.add(key);
    }
  }

  await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(scheduledIds));

  return {
    scheduled: true,
    count: scheduledIds.length,
  };
}

export async function scheduleTestNotification(): Promise<TestNotificationResult> {
  const Notifications = await getNotifications();

  if (!Notifications) {
    return {
      scheduled: false,
      reason: EXPO_GO_REASON,
    };
  }

  const identifier = await Notifications.scheduleNotificationAsync({
    content: {
      title: "Prueba CuidaVoz",
      body: "Las notificaciones están funcionando correctamente.",
      data: {
        type: "test_notification",
      },
      sound: "default",
    },
    trigger: {
      type: Notifications.SchedulableTriggerInputTypes.TIME_INTERVAL,
      channelId: CHANNEL_ID,
      seconds: 10,
      repeats: false,
    },
  });

  return {
    scheduled: true,
    identifier,
  };
}

async function getStoredNotificationIds() {
  const storedValue = await AsyncStorage.getItem(STORAGE_KEY);
  if (!storedValue) {
    return [];
  }

  try {
    const parsed = JSON.parse(storedValue) as unknown;
    return Array.isArray(parsed)
      ? parsed.filter((value): value is string => typeof value === "string")
      : [];
  } catch {
    return [];
  }
}

function isValidScheduleTime(time: string) {
  return /^([01]\d|2[0-3]):[0-5]\d$/.test(time);
}

function parseScheduleTime(time: string) {
  const [hour = "0", minute = "0"] = time.split(":");
  return [Number(hour), Number(minute)] as const;
}
