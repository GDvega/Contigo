import { CircleAlert, CircleCheckBig, Pill } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { Medication } from "@/types";

type MedicationStatusListProps = {
  medications: Medication[];
};

export function MedicationStatusList({
  medications,
}: MedicationStatusListProps) {
  return (
    <Card className="rounded-3xl border-none">
      <CardHeader>
        <CardTitle className="text-2xl">Estado de medicamentos</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3 pb-6">
        {medications.map((medication) => {
          const taken = medication.status === "taken";

          return (
            <div
              key={medication.id}
              className="flex items-center justify-between rounded-2xl border border-border/80 bg-background px-4 py-4"
            >
              <div className="flex items-center gap-3">
                <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-muted">
                  <Pill className="size-5 text-primary" aria-hidden="true" />
                </div>
                <div>
                  <p className="text-lg font-semibold">{medication.name}</p>
                  <p className="text-sm text-muted-foreground">{medication.time}</p>
                </div>
              </div>
              <Badge
                variant={taken ? "default" : "outline"}
                className="h-auto rounded-full px-3 py-2 text-sm"
              >
                {taken ? (
                  <CircleCheckBig className="size-4" aria-hidden="true" />
                ) : (
                  <CircleAlert className="size-4" aria-hidden="true" />
                )}
                {taken ? "Tomado" : "Pendiente"}
              </Badge>
            </div>
          );
        })}
      </CardContent>
    </Card>
  );
}
