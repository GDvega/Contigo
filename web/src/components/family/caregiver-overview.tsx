import { Clock3, ShieldCheck, UserRound } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { BloodPressureReading, PatientProfile } from "@/types";

type CaregiverOverviewProps = {
  patient: PatientProfile;
  latestReading: BloodPressureReading;
  weeklyAdherence: number;
};

export function CaregiverOverview({
  patient,
  latestReading,
  weeklyAdherence,
}: CaregiverOverviewProps) {
  return (
    <Card className="rounded-3xl border-none ring-1 ring-primary/10">
      <CardHeader className="space-y-2">
        <div className="inline-flex w-fit items-center gap-2 rounded-full bg-secondary px-4 py-2 text-sm font-medium text-secondary-foreground">
          <UserRound className="size-4" aria-hidden="true" />
          Panel familiar
        </div>
        <CardTitle className="text-3xl">María Rojas, {patient.age} años</CardTitle>
      </CardHeader>
      <CardContent className="grid gap-4 pb-6 sm:grid-cols-2">
        <div className="rounded-2xl bg-muted p-4">
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-muted-foreground">
            Última presión
          </p>
          <p className="mt-2 text-3xl font-semibold">
            {latestReading.systolic}/{latestReading.diastolic}
          </p>
          <p className="mt-2 flex items-center gap-2 text-base text-muted-foreground">
            <Clock3 className="size-4" aria-hidden="true" />
            Hoy {latestReading.time}
          </p>
        </div>
        <div className="rounded-2xl bg-accent p-4">
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-muted-foreground">
            Adherencia semanal
          </p>
          <p className="mt-2 text-3xl font-semibold">{weeklyAdherence}%</p>
          <p className="mt-2 flex items-center gap-2 text-base text-foreground/80">
            <ShieldCheck className="size-4" aria-hidden="true" />
            Seguimiento estable esta semana
          </p>
        </div>
      </CardContent>
    </Card>
  );
}
