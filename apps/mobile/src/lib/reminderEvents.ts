import type { DailyMedication, MedicationGroup } from "@/types";

export type ReminderEvent = {
  medicationIds: string[];
  medicationNames: string[];
  medicationDoses: string[];
  scheduleTime: string;
  scheduledFor: string;
  totalMedications: number;
  source: "notification_received" | "notification_opened";
};

type ReminderSubscriber = (event: ReminderEvent) => void;

let latestReminderEvent: ReminderEvent | null = null;
const subscribers = new Set<ReminderSubscriber>();

export function publishReminderEvent(event: ReminderEvent) {
  latestReminderEvent = event;
  subscribers.forEach((subscriber) => subscriber(event));
}

export function consumeLatestReminderEvent() {
  const event = latestReminderEvent;
  latestReminderEvent = null;
  return event;
}

export function subscribeToReminderEvents(subscriber: ReminderSubscriber) {
  subscribers.add(subscriber);
  return () => {
    subscribers.delete(subscriber);
  };
}

export function buildReminderEventFromMedicationGroup(
  group: MedicationGroup | { medications: DailyMedication[]; scheduleTime: string },
  source: ReminderEvent["source"]
): ReminderEvent {
  return {
    medicationIds: group.medications.map((medication) => medication.id),
    medicationNames: group.medications.map((medication) => medication.name),
    medicationDoses: group.medications.map((medication) => medication.dose),
    scheduleTime: group.scheduleTime,
    scheduledFor: new Date().toISOString(),
    totalMedications: group.medications.length,
    source,
  };
}
