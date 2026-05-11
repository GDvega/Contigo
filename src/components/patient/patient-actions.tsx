"use client";

import { BellRing, HeartHandshake, ShieldPlus } from "lucide-react";
import { useState } from "react";

import { useHelpDialog } from "@/components/patient/help-dialog-provider";
import { PressureRegistrationDialog } from "@/components/patient/pressure-registration-dialog";
import { Button } from "@/components/ui/button";

const actions = [
  {
    label: "Ya tomé mi pastilla",
    icon: ShieldPlus,
    variant: "default" as const,
  },
  {
    label: "Registrar presión",
    icon: BellRing,
    variant: "outline" as const,
  },
  {
    label: "Pedir ayuda",
    icon: HeartHandshake,
    variant: "secondary" as const,
  },
];

type PatientActionsProps = {
  canConfirmMedication?: boolean;
  isConfirmingMedication?: boolean;
  medicationActionLabel?: string;
  onConfirmMedication?: () => void;
};

export function PatientActions({
  canConfirmMedication = true,
  isConfirmingMedication = false,
  medicationActionLabel = actions[0].label,
  onConfirmMedication,
}: PatientActionsProps) {
  const [isPressureDialogOpen, setIsPressureDialogOpen] = useState(false);
  const { openHelp } = useHelpDialog();

  return (
    <>
      <div className="grid gap-3">
        <Button
          variant={actions[0].variant}
          disabled={!canConfirmMedication || isConfirmingMedication}
          className="h-auto min-h-20 justify-start rounded-3xl bg-primary px-6 py-5 text-left text-xl font-semibold shadow-sm sm:text-2xl"
          onClick={onConfirmMedication}
        >
          <ShieldPlus className="mr-3 size-6 shrink-0" aria-hidden="true" />
          {isConfirmingMedication ? "Guardando..." : medicationActionLabel}
        </Button>

        <Button
          variant={actions[1].variant}
          className="h-auto min-h-16 justify-start rounded-3xl border-primary/15 bg-white px-5 py-4 text-left text-lg font-semibold text-slate-900 shadow-sm hover:bg-white/90 sm:text-xl"
          onClick={() => setIsPressureDialogOpen(true)}
        >
          <BellRing className="mr-3 size-6 shrink-0" aria-hidden="true" />
          {actions[1].label}
        </Button>

        <Button
          variant={actions[2].variant}
          className="h-auto min-h-16 justify-start rounded-3xl bg-[#fff6e6] px-5 py-4 text-left text-lg font-semibold text-amber-950 shadow-sm hover:bg-[#fff1d6] sm:text-xl"
          onClick={() => openHelp()}
        >
          <HeartHandshake className="mr-3 size-6 shrink-0" aria-hidden="true" />
          {actions[2].label}
        </Button>
      </div>

      <PressureRegistrationDialog
        open={isPressureDialogOpen}
        onOpenChange={setIsPressureDialogOpen}
      />
    </>
  );
}
