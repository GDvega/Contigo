const PERU_UTC_OFFSET_HOURS = -5;
const HOUR_IN_MS = 60 * 60 * 1000;
const DAY_IN_MS = 24 * HOUR_IN_MS;

function shiftToPeruClock(date: Date) {
  return new Date(date.getTime() + PERU_UTC_OFFSET_HOURS * HOUR_IN_MS);
}

export function getPeruDayWindow(referenceDate = new Date()) {
  const peruClock = shiftToPeruClock(referenceDate);
  const startOfPeruDay = new Date(
    Date.UTC(
      peruClock.getUTCFullYear(),
      peruClock.getUTCMonth(),
      peruClock.getUTCDate(),
      -PERU_UTC_OFFSET_HOURS,
      0,
      0,
      0
    )
  );

  return {
    startOfDay: startOfPeruDay,
    endOfDay: new Date(startOfPeruDay.getTime() + DAY_IN_MS),
  };
}

export function todayAtPeruTime(hours: number, minutes: number, referenceDate = new Date()) {
  const { startOfDay } = getPeruDayWindow(referenceDate);
  const scheduled = new Date(startOfDay);
  scheduled.setUTCHours(hours - PERU_UTC_OFFSET_HOURS, minutes, 0, 0);
  return scheduled;
}
