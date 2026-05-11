/* eslint-disable @next/next/no-img-element */

import { Pill } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { MedicationApi } from "@/components/medication/medication-management";

type MedicationListProps = {
  medications: MedicationApi[];
};

export function MedicationList({ medications }: MedicationListProps) {
  return (
    <Card className="rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10">
      <CardHeader>
        <CardTitle className="text-2xl">Lista de medicamentos</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3 pb-6">
        {medications.map((medication) => (
          <div
            key={medication.id}
            className="rounded-3xl border border-border/70 bg-[#fffdfa] p-4 shadow-sm"
          >
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-start gap-3">
                {medication.imageUrl ? (
                  <img
                    src={medication.imageUrl}
                    alt={`Imagen de ${medication.name}`}
                    className="h-20 w-20 shrink-0 rounded-2xl border border-border object-cover"
                  />
                ) : (
                  <div className="flex h-20 w-20 shrink-0 items-center justify-center rounded-2xl bg-secondary">
                    <Pill className="size-7 text-primary" aria-hidden="true" />
                  </div>
                )}
                <div>
                  <p className="text-lg font-semibold">{medication.name}</p>
                  <p className="text-sm text-muted-foreground">
                    {medication.dose} · {medication.schedules[0]?.time ?? "Sin hora"}
                  </p>
                </div>
              </div>
              {medication.color ? (
                <Badge variant="outline" className="rounded-full px-3 py-1.5 text-sm">
                  {medication.color}
                </Badge>
              ) : null}
            </div>
            <div className="mt-4 grid gap-2 text-base text-foreground/80">
              {medication.shape ? (
                <p>
                  <span className="font-semibold text-foreground">Forma:</span>{" "}
                  {medication.shape}
                </p>
              ) : null}
              {medication.instructions ? (
                <p>
                  <span className="font-semibold text-foreground">
                    Instrucciones:
                  </span>{" "}
                  {medication.instructions}
                </p>
              ) : null}
              <p className="text-sm text-muted-foreground">
                Paciente: {medication.patient.fullName}
              </p>
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
