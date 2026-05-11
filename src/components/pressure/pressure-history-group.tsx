import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { BloodPressureReadingApi } from "@/types";

import { PressureStatusBadge } from "./pressure-status-badge";

type PressureHistoryGroupProps = {
  date: string;
  readings: BloodPressureReadingApi[];
};

const dateTimeFormatter = new Intl.DateTimeFormat("es-PE", {
  dateStyle: "medium",
  timeStyle: "short",
});

export function PressureHistoryGroup({
  date,
  readings,
}: PressureHistoryGroupProps) {
  return (
    <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
      <CardHeader>
        <CardTitle className="text-2xl">{date}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3 pb-6">
        {readings.map((reading) => (
          <div
            key={reading.id}
            className="rounded-3xl border border-border/70 bg-[#fffdfa] px-4 py-4 shadow-sm"
          >
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="text-3xl font-semibold tracking-normal">
                  {reading.systolic}/{reading.diastolic}
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  {dateTimeFormatter.format(new Date(reading.measuredAt))}
                </p>
              </div>
              <PressureStatusBadge status={reading.status} />
            </div>
            <div className="mt-4 grid gap-2 text-base text-foreground/80">
              <p>
                <span className="font-semibold text-foreground">Paciente:</span>{" "}
                {reading.patient.fullName}
              </p>
              {reading.pulse ? (
                <p>
                  <span className="font-semibold text-foreground">Pulso:</span>{" "}
                  {reading.pulse} lpm
                </p>
              ) : null}
              {reading.notes ? (
                <p>
                  <span className="font-semibold text-foreground">Notas:</span>{" "}
                  {reading.notes}
                </p>
              ) : null}
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
