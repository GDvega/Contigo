"use client";

import dynamic from "next/dynamic";

const VoicePressureButton = dynamic(
  () =>
    import("@/components/voice/VoicePressureButton").then(
      (mod) => mod.VoicePressureButton
    ),
  {
    ssr: false,
    loading: () => (
      <div className="rounded-3xl bg-primary px-6 py-6 text-primary-foreground">
        <p className="text-sm font-semibold opacity-90">Voz</p>
        <p className="mt-2 text-2xl font-semibold">Preparando micrófono...</p>
      </div>
    ),
  }
);

export function VoicePressureClient() {
  return (
    <section id="voice-pressure-section" className="scroll-mt-6">
      <VoicePressureButton />
    </section>
  );
}
