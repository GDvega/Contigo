/* eslint-disable @next/next/no-img-element */

import { Clock3, Pill } from "lucide-react";

import { Card, CardContent, CardTitle } from "@/components/ui/card";

type NextMedicationCardProps = {
  title?: string;
  urgent?: boolean;
  medication: {
    name: string;
    dose: string;
    time: string;
    color?: string | null;
    shape?: string | null;
    instructions?: string | null;
    imageUrl?: string | null;
  };
};

export function NextMedicationCard({
  title = "Próxima medicina",
  urgent = false,
  medication,
}: NextMedicationCardProps) {
  return (
    <Card
      className={
        urgent
          ? "rounded-[2rem] border border-amber-200 bg-[#fff4d8] shadow-sm ring-1 ring-amber-100"
          : "rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10"
      }
    >
      <CardContent className="space-y-5 px-5 py-6">
        <p
          className={
            urgent
              ? "text-sm font-semibold uppercase tracking-[0.18em] text-amber-900"
              : "text-sm font-semibold uppercase tracking-[0.18em] text-primary"
          }
        >
          {title}
        </p>

        <div className="flex items-start gap-4 min-[390px]:gap-5">
          {medication.imageUrl ? (
            <img
              src={medication.imageUrl}
              alt={`Imagen de ${medication.name}`}
              className="h-24 w-24 shrink-0 rounded-3xl border border-black/5 object-cover shadow-md min-[390px]:h-32 min-[390px]:w-32"
            />
          ) : (
            <div className="flex h-24 w-24 shrink-0 items-center justify-center rounded-3xl bg-white shadow-md ring-1 ring-black/5 min-[390px]:h-32 min-[390px]:w-32">
              <Pill className="size-11 text-primary" aria-hidden="true" />
            </div>
          )}

          <div className="min-w-0 flex-1 space-y-3">
            <CardTitle className="text-3xl leading-tight tracking-tight min-[390px]:text-4xl">
              {medication.name}
            </CardTitle>

            <div className="grid gap-2 text-lg font-semibold text-foreground/90 min-[390px]:text-xl">
              <p className="inline-flex w-fit items-center gap-2 rounded-2xl bg-white px-3 py-2 shadow-sm ring-1 ring-black/5">
                <Clock3 className="size-5 shrink-0" aria-hidden="true" />
                {medication.time}
              </p>
              <p className="inline-flex w-fit items-center gap-2 rounded-2xl bg-white px-3 py-2 shadow-sm ring-1 ring-black/5">
                <Pill className="size-5 shrink-0" aria-hidden="true" />
                {medication.dose}
              </p>
            </div>

            <div className="space-y-2 text-lg leading-7 text-foreground/90 min-[390px]:text-xl">
              <p className="font-semibold">
                {[medication.color, medication.shape].filter(Boolean).join(", ")}
              </p>
              {medication.instructions ? <p>{medication.instructions}</p> : null}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
