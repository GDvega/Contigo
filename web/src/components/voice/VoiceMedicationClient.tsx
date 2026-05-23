"use client";

import dynamic from "next/dynamic";

type VoiceMedicationClientProps = {
  medicationId: string;
  medicationName: string;
  scheduleTime: string;
  onSaved?: () => void;
};

const VoiceMedicationButton = dynamic(
  () =>
    import("@/components/voice/VoiceMedicationButton").then(
      (mod) => mod.VoiceMedicationButton
    ),
  {
    ssr: false,
    loading: () => (
      <div className="rounded-3xl bg-muted px-6 py-6">
        <p className="text-sm font-semibold text-muted-foreground">Voz</p>
        <p className="mt-2 text-2xl font-semibold">Preparando confirmación...</p>
      </div>
    ),
  }
);

export function VoiceMedicationClient(props: VoiceMedicationClientProps) {
  return <VoiceMedicationButton {...props} />;
}
