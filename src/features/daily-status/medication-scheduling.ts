import type { DailyStatus } from "@/features/daily-status/daily-status.types";

export type DailyMedication = DailyStatus["medications"][number];

export function minutesFromScheduleTime(time: string) {
  const normalizedTime = time.trim().toUpperCase();
  const match = normalizedTime.match(/^(\d{1,2}):(\d{2})(?:\s*(AM|PM))?$/);

  if (!match) {
    return null;
  }

  let hours = Number(match[1]);
  const minutes = Number(match[2]);
  const meridiem = match[3];

  if (Number.isNaN(hours) || Number.isNaN(minutes)) {
    return null;
  }

  if (meridiem === "PM" && hours < 12) {
    hours += 12;
  }

  if (meridiem === "AM" && hours === 12) {
    hours = 0;
  }

  if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
    return null;
  }

  return hours * 60 + minutes;
}

export function isMedicationDue(medication: DailyMedication, now: Date) {
  if (medication.statusToday !== "PENDING" || !medication.scheduleTime) {
    return false;
  }

  const scheduleMinutes = minutesFromScheduleTime(medication.scheduleTime);

  if (scheduleMinutes === null) {
    return false;
  }

  return now.getHours() * 60 + now.getMinutes() >= scheduleMinutes;
}

export function getMedicationToRegister(
  medications: DailyMedication[],
  now: Date
) {
  return (
    medications.find((medication) => isMedicationDue(medication, now)) ??
    medications.find((medication) => medication.statusToday === "PENDING") ??
    null
  );
}

export function scheduledForTodayIso(time: string) {
  const date = new Date();
  const scheduleMinutes = minutesFromScheduleTime(time);

  if (scheduleMinutes === null) {
    return date.toISOString();
  }

  const hours = Math.floor(scheduleMinutes / 60);
  const minutes = scheduleMinutes % 60;
  date.setHours(hours, minutes, 0, 0);
  return date.toISOString();
}
