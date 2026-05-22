import AsyncStorage from "@react-native-async-storage/async-storage";
import Constants, { ExecutionEnvironment } from "expo-constants";
import { Platform } from "react-native";

import {
  createNotificationSchedule,
  deleteAllNotificationSchedules,
  deleteNotificationSchedulesByIds,
  getAllNotificationSchedules,
  getNotificationSchedulesForMedicationDate,
  getMedications,
} from "@/lib/localRepositories";
import { publishReminderEvent, type ReminderEvent } from "@/lib/reminderEvents";
import {
  getSpokenReminderSupport,
  speakMedicationReminderWithBestAvailableBackend,
  speakOpenedMedicationReminderWithBestAvailableBackend,
} from "@/lib/spokenReminders";
import { navigationRef } from "@/navigation/navigationRef";
import { scheduleNativeAlarm, cancelNativeAlarm } from "@/native/CuidaVozNative";
import type { Medication } from "@/types";

const REMINDERS_ENABLED_KEY = "cuidavoz.remindersEnabled";
const CHANNEL_ID = "medication-reminders";
const EXPO_GO_REASON = "expo_go_not_supported";
const DEFAULT_REPEAT_EVERY_MINUTES = 10;
const DEFAULT_REPEAT_COUNT = 3;
const DEFAULT_SPEAK_ON_OPEN = true;
const DEFAULT_FOREGROUND_SPEAK_COUNT = 2;
const LOOKAHEAD_DAYS = 30;

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
      count: number;
    }
  | {
      cancelled: false;
      reason: string;
    };

type ReminderNotificationData = {
  type?: string;
  medicationIds?: string[];
  medicationNames?: string[];
  medicationDoses?: string[];
  scheduleTime?: string;
  scheduledFor?: string;
};

type MedicationScheduleGroup = {
  scheduleTime: string;
  medications: Array<{
    id: string;
    name: string;
    dose: string;
  }>;
};

let notificationsModule: ExpoNotifications | null = null;
let cleanupListeners: (() => void) | null = null;
let lastHandledResponseIdentifier: string | null = null;

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

function isValidScheduleTime(time: string) {
  return /^([01]\d|2[0-3]):[0-5]\d$/.test(time);
}

function parseScheduleTime(time: string) {
  const [hour = "0", minute = "0"] = time.split(":");
  return [Number(hour), Number(minute)] as const;
}

function toIsoDate(date: Date) {
  return date.toISOString().slice(0, 10);
}

function startOfDay(date = new Date()) {
  const next = new Date(date);
  next.setHours(0, 0, 0, 0);
  return next;
}

function addMinutes(date: Date, minutes: number) {
  return new Date(date.getTime() + minutes * 60_000);
}

function addDays(date: Date, days: number) {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
}

function createLocalDateFromIsoDate(date: string) {
  const [year = "1970", month = "1", day = "1"] = date.split("-");
  return new Date(Number(year), Number(month) - 1, Number(day), 0, 0, 0, 0);
}

function createScheduledDate(baseDate: Date, time: string) {
  const [hour, minute] = parseScheduleTime(time);
  const scheduled = new Date(baseDate);
  scheduled.setHours(hour, minute, 0, 0);
  return scheduled;
}

function shouldSkipPastReminder(scheduled: Date) {
  return scheduled.getTime() <= Date.now();
}

function buildMedicationNamesText(names: string[]) {
  if (names.length <= 1) {
    return names[0] ?? "";
  }

  if (names.length === 2) {
    return `${names[0]} y ${names[1]}`;
  }

  return `${names.slice(0, -1).join(", ")} y ${names[names.length - 1]}`;
}

function buildReminderBody(group: MedicationScheduleGroup) {
  if (group.medications.length === 1) {
    const medication = group.medications[0];
    return `${medication.name} · ${medication.dose}. Abre CuidaVoz y di: ya tomé mi pastilla.`;
  }

  return `Debes tomar ${buildMedicationNamesText(
    group.medications.map((medication) => medication.name)
  )}. Abre CuidaVoz y di: ya tomé mis pastillas.`;
}

function normalizeReminderEvent(data: ReminderNotificationData, source: ReminderEvent["source"]) {
  if (
    data.type !== "medication_group_reminder" ||
    !Array.isArray(data.medicationIds) ||
    !Array.isArray(data.medicationNames) ||
    !Array.isArray(data.medicationDoses) ||
    data.medicationIds.length === 0 ||
    !data.scheduleTime ||
    !data.scheduledFor
  ) {
    return null;
  }

  return {
    medicationIds: data.medicationIds,
    medicationNames: data.medicationNames,
    medicationDoses: data.medicationDoses,
    scheduleTime: data.scheduleTime,
    scheduledFor: data.scheduledFor,
    totalMedications: data.medicationIds.length,
    source,
  } satisfies ReminderEvent;
}

function groupMedicationsByScheduleTime(medications: Medication[]) {
  const scheduleGroups = new Map<string, MedicationScheduleGroup["medications"]>();

  for (const medication of medications) {
    if (medication.isActive === false) {
      continue;
    }

    for (const schedule of medication.schedules.filter(
      (item) => item.isActive && isValidScheduleTime(item.time)
    )) {
      const list = scheduleGroups.get(schedule.time) ?? [];
      list.push({
        id: medication.id,
        name: medication.name,
        dose: medication.dose,
      });
      scheduleGroups.set(schedule.time, list);
    }
  }

  return [...scheduleGroups.entries()]
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([scheduleTime, groupedMedications]) => ({
      scheduleTime,
      medications: groupedMedications.sort((left, right) =>
        left.name.localeCompare(right.name, "es")
      ),
    }));
}

export function buildGroupFromReminderEvent(event: ReminderEvent): MedicationScheduleGroup {
  return {
    scheduleTime: event.scheduleTime,
    medications: event.medicationIds.map((medicationId, index) => ({
      id: medicationId,
      name: event.medicationNames[index] ?? "Pastilla",
      dose: event.medicationDoses[index] ?? "",
    })),
  };
}

async function ensureAndroidChannel(Notifications: ExpoNotifications) {
  if (Platform.OS !== "android") {
    return;
  }

  await Notifications.setNotificationChannelAsync(CHANNEL_ID, {
    name: "Recordatorios de pastillas",
    importance: Notifications.AndroidImportance.MAX,
    vibrationPattern: [0, 800, 400, 800],
    sound: "default",
  });
}

export async function speakMedicationReminder(
  group: MedicationScheduleGroup,
  repeatCount = DEFAULT_FOREGROUND_SPEAK_COUNT
) {
  await speakMedicationReminderWithBestAvailableBackend(group, repeatCount);
}

export async function speakOpenedMedicationReminder(
  group: MedicationScheduleGroup,
  repeatCount = DEFAULT_FOREGROUND_SPEAK_COUNT
) {
  await speakOpenedMedicationReminderWithBestAvailableBackend(group, repeatCount);
}

export async function configureNotificationBehavior() {
  const Notifications = await getNotifications();

  if (!Notifications) {
    return {
      configured: false,
      reason: EXPO_GO_REASON,
    };
  }

  await ensureAndroidChannel(Notifications);

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

export async function cancelMedicationReminderNotifications(
  medicationId: string,
  date = toIsoDate(new Date())
): Promise<NotificationCancelResult> {
  const Notifications = await getNotifications();

  if (!Notifications) {
    return {
      cancelled: false,
      reason: EXPO_GO_REASON,
    };
  }

  const schedules = await getNotificationSchedulesForMedicationDate(medicationId, date);

  const notificationIds = [...new Set(schedules.map((schedule) => schedule.notificationId))];

  await Promise.all(
    notificationIds.map((notificationId) => {
      if (Platform.OS === "android") {
        cancelNativeAlarm(`native_${notificationId}`);
      }
      return Notifications.cancelScheduledNotificationAsync(notificationId).catch(
        () => undefined
      );
    })
  );

  await deleteNotificationSchedulesByIds(notificationIds);

  return {
    cancelled: true,
    count: notificationIds.length,
  };
}

export async function cancelMedicationGroupReminderNotifications(
  medicationIds: string[],
  scheduleTime?: string,
  date = toIsoDate(new Date())
): Promise<NotificationCancelResult> {
  if (medicationIds.length === 0) {
    return {
      cancelled: true,
      count: 0,
    };
  }

  const Notifications = await getNotifications();

  if (!Notifications) {
    return {
      cancelled: false,
      reason: EXPO_GO_REASON,
    };
  }

  const schedules = (
    await Promise.all(
      medicationIds.map((medicationId) =>
        getNotificationSchedulesForMedicationDate(medicationId, date)
      )
    )
  ).flat();

  const filteredSchedules = !scheduleTime
    ? schedules
    : (() => {
        const baseDate = createLocalDateFromIsoDate(date);
        const allowedTimes = new Set(
          Array.from({ length: DEFAULT_REPEAT_COUNT }, (_, index) =>
            addMinutes(
              createScheduledDate(baseDate, scheduleTime),
              index * DEFAULT_REPEAT_EVERY_MINUTES
            ).toISOString()
          )
        );

        return schedules.filter((schedule) => allowedTimes.has(schedule.scheduledFor));
      })();

  const notificationIds = [
    ...new Set(filteredSchedules.map((schedule) => schedule.notificationId)),
  ];

  await Promise.all(
    notificationIds.map((notificationId) => {
      if (Platform.OS === "android") {
        cancelNativeAlarm(`native_${notificationId}`);
      }
      return Notifications.cancelScheduledNotificationAsync(notificationId).catch(
        () => undefined
      );
    })
  );

  await deleteNotificationSchedulesByIds(notificationIds);

  return {
    cancelled: true,
    count: notificationIds.length,
  };
}

export async function cancelAllMedicationReminderNotifications(): Promise<NotificationCancelResult> {
  const Notifications = await getNotifications();

  if (!Notifications) {
    return {
      cancelled: false,
      reason: EXPO_GO_REASON,
    };
  }

  const schedules = await getAllNotificationSchedules();

  await Promise.all(
    schedules.map((schedule) =>
      Notifications.cancelScheduledNotificationAsync(schedule.notificationId).catch(
        () => undefined
      )
    )
  );

  const pendingNotifications = await Notifications.getAllScheduledNotificationsAsync();
  const reminderNotifications = pendingNotifications.filter(
    (notification) => notification.content.data?.type === "medication_group_reminder"
  );

  await Promise.all(
    reminderNotifications.map((notification) =>
      Notifications.cancelScheduledNotificationAsync(notification.identifier).catch(
        () => undefined
      )
    )
  );

  await deleteAllNotificationSchedules();

  return {
    cancelled: true,
    count: schedules.length,
  };
}

async function scheduleSingleReminder(
  Notifications: ExpoNotifications,
  group: MedicationScheduleGroup,
  scheduledDate: Date
) {
  const scheduledFor = scheduledDate.toISOString();
  const identifier = await Notifications.scheduleNotificationAsync({
    content: {
      title:
        group.medications.length === 1
          ? "Hora de tomar tu pastilla"
          : "Hora de tomar tus pastillas",
      body: buildReminderBody(group),
      data: {
        type: "medication_group_reminder",
        medicationIds: group.medications.map((medication) => medication.id),
        medicationNames: group.medications.map((medication) => medication.name),
        medicationDoses: group.medications.map((medication) => medication.dose),
        scheduleTime: group.scheduleTime,
        scheduledFor,
      },
      sound: "default",
    },
    trigger: {
      type: Notifications.SchedulableTriggerInputTypes.DATE,
      channelId: CHANNEL_ID,
      date: scheduledDate,
    },
  });

  // Integración con AlarmManager Nativo (Android)
  if (Platform.OS === "android") {
    const alarmId = `native_${identifier}`;
    const ttsMessage = `Es hora de tomar ${buildMedicationNamesText(
      group.medications.map((m) => m.name)
    )}`;
    await scheduleNativeAlarm(alarmId, scheduledDate.getTime(), ttsMessage);
  }

  for (const medication of group.medications) {
    await createNotificationSchedule({
      medicationId: medication.id,
      scheduledFor,
      notificationId: identifier,
      type: "medication_reminder",
    });
  }

  return identifier;
}

async function scheduleReminderSeries(
  Notifications: ExpoNotifications,
  group: MedicationScheduleGroup
) {
  const today = startOfDay();
  let count = 0;

  for (let dayOffset = 0; dayOffset < LOOKAHEAD_DAYS; dayOffset += 1) {
    const day = addDays(today, dayOffset);
    const firstReminder = createScheduledDate(day, group.scheduleTime);

    for (let repeatIndex = 0; repeatIndex < DEFAULT_REPEAT_COUNT; repeatIndex += 1) {
      const scheduledDate = addMinutes(
        firstReminder,
        repeatIndex * DEFAULT_REPEAT_EVERY_MINUTES
      );

      if (dayOffset === 0 && shouldSkipPastReminder(scheduledDate)) {
        continue;
      }

      await scheduleSingleReminder(Notifications, group, scheduledDate);
      count += 1;
    }
  }

  return count;
}

export async function scheduleMedicationReminders(): Promise<NotificationScheduleResult> {
  const Notifications = await getNotifications();

  if (!Notifications) {
    return {
      scheduled: false,
      reason: EXPO_GO_REASON,
    };
  }

  await configureNotificationBehavior();
  await cancelAllMedicationReminderNotifications();

  const groupedMedications = groupMedicationsByScheduleTime(await getMedications());
  let count = 0;

  for (const group of groupedMedications) {
    count += await scheduleReminderSeries(Notifications, group);
  }

  await AsyncStorage.setItem(REMINDERS_ENABLED_KEY, "true");

  return {
    scheduled: true,
    count,
  };
}

export async function rescheduleAllMedicationReminders() {
  if (!(await areMedicationRemindersEnabled())) {
    return {
      scheduled: true,
      count: 0,
    } satisfies NotificationScheduleResult;
  }

  return scheduleMedicationReminders();
}

export async function scheduleMedicationSnooze(
  reminderTarget: {
    medicationIds: string[];
    medicationNames: string[];
    medicationDoses: string[];
    scheduleTime: string;
  },
  minutes = DEFAULT_REPEAT_EVERY_MINUTES
) {
  const Notifications = await getNotifications();

  if (!Notifications) {
    return {
      scheduled: false,
      reason: EXPO_GO_REASON,
    } satisfies NotificationScheduleResult;
  }

  const scheduledDate = addMinutes(new Date(), minutes);
  await scheduleSingleReminder(
    Notifications,
    {
      scheduleTime: reminderTarget.scheduleTime,
      medications: reminderTarget.medicationIds.map((medicationId, index) => ({
        id: medicationId,
        name: reminderTarget.medicationNames[index] ?? "Pastilla",
        dose: reminderTarget.medicationDoses[index] ?? "",
      })),
    },
    scheduledDate
  );

  return {
    scheduled: true,
    count: 1,
  } satisfies NotificationScheduleResult;
}

export async function handleNotificationResponse(
  response: {
    notification: {
      request: {
        identifier?: string;
        content: { data: ReminderNotificationData };
      };
    };
  }
) {
  const identifier = response.notification.request.identifier ?? null;

  if (identifier && identifier === lastHandledResponseIdentifier) {
    return;
  }

  const event = normalizeReminderEvent(
    response.notification.request.content.data,
    "notification_opened"
  );

  if (!event) {
    return;
  }

  lastHandledResponseIdentifier = identifier;
  publishReminderEvent(event);

  if (navigationRef.isReady()) {
    navigationRef.navigate("Inicio");
  }
}

export async function attachNotificationListeners() {
  const Notifications = await getNotifications();

  if (!Notifications || cleanupListeners) {
    return cleanupListeners;
  }

  const receivedSubscription = Notifications.addNotificationReceivedListener(
    (notification) => {
      const event = normalizeReminderEvent(
        notification.request.content.data as ReminderNotificationData,
        "notification_received"
      );

      if (!event) {
        return;
      }

      publishReminderEvent(event);
      void speakMedicationReminder(buildGroupFromReminderEvent(event));
    }
  );

  const responseSubscription = Notifications.addNotificationResponseReceivedListener(
    (response) => {
      void handleNotificationResponse(response);
    }
  );

  cleanupListeners = () => {
    receivedSubscription.remove();
    responseSubscription.remove();
    cleanupListeners = null;
  };

  return cleanupListeners;
}

export async function scheduleTestNotification(): Promise<TestNotificationResult> {
  const Notifications = await getNotifications();

  if (!Notifications) {
    return {
      scheduled: false,
      reason: EXPO_GO_REASON,
    };
  }

  await configureNotificationBehavior();

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

export async function areMedicationRemindersEnabled() {
  const value = await AsyncStorage.getItem(REMINDERS_ENABLED_KEY);
  return value === "true";
}

export async function disableMedicationRemindersFlag() {
  await AsyncStorage.removeItem(REMINDERS_ENABLED_KEY);
}

export async function setMedicationRemindersEnabled(enabled: boolean) {
  if (enabled) {
    await AsyncStorage.setItem(REMINDERS_ENABLED_KEY, "true");
    return;
  }

  await AsyncStorage.removeItem(REMINDERS_ENABLED_KEY);
}

export function getReminderSettingsSummary() {
  const spokenSupport = getSpokenReminderSupport();

  return {
    repeatEveryMinutes: DEFAULT_REPEAT_EVERY_MINUTES,
    repeatCount: DEFAULT_REPEAT_COUNT,
    speakOnOpen: DEFAULT_SPEAK_ON_OPEN,
    spokenBackend: spokenSupport.backend,
    spokenLevel: spokenSupport.level,
    spokenModeLabel: spokenSupport.modeLabel,
    spokenDetail: spokenSupport.detail,
  };
}
