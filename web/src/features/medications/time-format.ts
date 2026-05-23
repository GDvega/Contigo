export type MedicationTimePeriod = "AM" | "PM";

export type MedicationTimeParts = {
  hour: string;
  minute: string;
  period: MedicationTimePeriod;
};

export function to24HourTime(
  hour: string,
  minute: string,
  period: MedicationTimePeriod
) {
  const parsedHour = Number(hour);
  const normalizedMinute = minute.padStart(2, "0");
  let normalizedHour = parsedHour % 12;

  if (period === "PM") {
    normalizedHour += 12;
  }

  return `${normalizedHour.toString().padStart(2, "0")}:${normalizedMinute}`;
}

export function from24HourTime(time: string): MedicationTimeParts {
  const match = time.match(/^([01]\d|2[0-3]):([0-5]\d)$/);

  if (!match) {
    return {
      hour: "",
      minute: "00",
      period: "AM",
    };
  }

  const hour24 = Number(match[1]);
  const minute = match[2];
  const period: MedicationTimePeriod = hour24 >= 12 ? "PM" : "AM";
  const hour12 = hour24 % 12 || 12;

  return {
    hour: hour12.toString(),
    minute,
    period,
  };
}

export function isValidMedicationTimeParts({
  hour,
  minute,
  period,
}: MedicationTimeParts) {
  const parsedHour = Number(hour);
  const parsedMinute = Number(minute);

  return (
    Number.isInteger(parsedHour) &&
    parsedHour >= 1 &&
    parsedHour <= 12 &&
    Number.isInteger(parsedMinute) &&
    parsedMinute >= 0 &&
    parsedMinute <= 55 &&
    parsedMinute % 5 === 0 &&
    (period === "AM" || period === "PM")
  );
}
