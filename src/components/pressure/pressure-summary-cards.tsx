import { Activity, CalendarDays, HeartPulse } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";
import type { BloodPressureReadingApi } from "@/types";

type PressureSummaryCardsProps = {
  readings: Pick<
    BloodPressureReadingApi,
    "systolic" | "diastolic" | "status"
  >[];
};

export function PressureSummaryCards({
  readings,
}: PressureSummaryCardsProps) {
  const averageSystolic = Math.round(
    readings.reduce((sum, reading) => sum + reading.systolic, 0) / readings.length
  );
  const averageDiastolic = Math.round(
    readings.reduce((sum, reading) => sum + reading.diastolic, 0) / readings.length
  );
  const highCount = readings.filter((reading) =>
    ["HIGH", "CRITICAL"].includes(reading.status.toUpperCase())
  ).length;

  const cards = [
    {
      label: "Promedio",
      value: `${averageSystolic}/${averageDiastolic}`,
      icon: Activity,
    },
    {
      label: "Lecturas",
      value: `${readings.length} registros`,
      icon: CalendarDays,
    },
    {
      label: "Alertas altas",
      value: `${highCount} esta semana`,
      icon: HeartPulse,
    },
  ];

  return (
    <div className="grid gap-3 sm:grid-cols-3">
      {cards.map(({ label, value, icon: Icon }) => (
        <Card
          key={label}
          className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10"
        >
          <CardContent className="flex items-start gap-3 px-5 py-5">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-secondary">
              <Icon className="size-5 text-primary" aria-hidden="true" />
            </div>
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.15em] text-muted-foreground">
                {label}
              </p>
              <p className="mt-1 text-2xl font-semibold">{value}</p>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
