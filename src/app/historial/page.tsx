import type { Metadata } from "next";

import { BloodPressureHistory } from "@/components/pressure/blood-pressure-history";

export const metadata: Metadata = {
  title: "Historial | CuidaVoz",
};

export default function HistoryPage() {
  return (
    <main className="mx-auto flex min-h-screen w-full max-w-5xl flex-col gap-5 bg-[#fbf7ef] px-4 py-6 text-slate-950 sm:px-6">
      <section className="space-y-3">
        <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary">
          Presión arterial
        </p>
        <h1 className="text-4xl font-semibold tracking-tight">Historial</h1>
        <p className="max-w-2xl text-lg leading-7 text-muted-foreground">
          Lecturas agrupadas por fecha con una lectura rápida del estado.
        </p>
      </section>

      <BloodPressureHistory />
    </main>
  );
}
