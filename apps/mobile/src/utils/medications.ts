import type { DailyMedication, MedicationGroup } from "@/types";
import { isMedicationDue } from "@/utils/dates";

function compareScheduleTimes(left: string, right: string) {
  return left.localeCompare(right);
}

export function getMedicationGroupsForToday(
  medications: DailyMedication[]
): MedicationGroup[] {
  const groups = new Map<string, DailyMedication[]>();

  for (const medication of medications) {
    const list = groups.get(medication.scheduleTime) ?? [];
    list.push(medication);
    groups.set(medication.scheduleTime, list);
  }

  return [...groups.entries()]
    .sort(([left], [right]) => compareScheduleTimes(left, right))
    .map(([scheduleTime, groupMedications]) => {
      const sortedMedications = [...groupMedications].sort((left, right) =>
        left.name.localeCompare(right.name, "es")
      );
      const takenMedications = sortedMedications.filter(
        (medication) => medication.statusToday === "TAKEN"
      ).length;
      const totalMedications = sortedMedications.length;
      const pendingMedications = totalMedications - takenMedications;

      return {
        scheduleTime,
        medications: sortedMedications,
        totalMedications,
        pendingMedications,
        takenMedications,
        isDue: isMedicationDue(scheduleTime),
        allTaken: pendingMedications === 0,
      };
    });
}

export function getNextMedicationGroup(medications: DailyMedication[]) {
  const pendingGroups = getMedicationGroupsForToday(medications).filter(
    (group) => !group.allTaken
  );

  return pendingGroups.find((group) => group.isDue) ?? pendingGroups[0] ?? null;
}

export function getMedicationGroupByScheduleTime(
  medications: DailyMedication[],
  scheduleTime: string
) {
  return (
    getMedicationGroupsForToday(medications).find(
      (group) => group.scheduleTime === scheduleTime
    ) ?? null
  );
}

export function getMedicationToRegister(medications: DailyMedication[]) {
  const nextGroup = getNextMedicationGroup(medications);

  if (!nextGroup) {
    return null;
  }

  return (
    nextGroup.medications.find((medication) => medication.statusToday === "PENDING") ??
    null
  );
}

export function formatMedicationGroupTitle(group: MedicationGroup) {
  return group.pendingMedications <= 1
    ? "Es hora de tomar tu pastilla"
    : "Es hora de tomar tus pastillas";
}
