import type { DailyMedication } from "@/types";
import { isMedicationDue } from "@/utils/dates";

export function getMedicationToRegister(medications: DailyMedication[]) {
  const pending = medications.filter(
    (medication) => medication.statusToday === "PENDING"
  );

  return (
    pending.find((medication) => isMedicationDue(medication.scheduleTime)) ??
    pending[0] ??
    null
  );
}
