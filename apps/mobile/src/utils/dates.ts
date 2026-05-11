export function scheduledForTodayIso(time: string) {
  const [hours = "0", minutes = "0"] = time.split(":");
  const date = new Date();
  date.setHours(Number(hours), Number(minutes), 0, 0);
  return date.toISOString();
}

export function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("es-PE", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function isMedicationDue(scheduleTime: string) {
  const [hours = "0", minutes = "0"] = scheduleTime.split(":");
  const scheduled = Number(hours) * 60 + Number(minutes);
  const now = new Date();
  const current = now.getHours() * 60 + now.getMinutes();
  return current >= scheduled;
}
