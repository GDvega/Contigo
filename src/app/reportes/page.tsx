import type { Metadata } from "next";

import { ReportsDashboard } from "@/components/reports/reports-dashboard";

export const metadata: Metadata = {
  title: "Reportes | CuidaVoz",
};

export default function ReportsPage() {
  return (
    <main className="mx-auto flex min-h-screen w-full max-w-5xl flex-col gap-5 bg-[#fbf7ef] px-4 py-6 text-slate-950 sm:px-6">
      <section className="space-y-3">
        <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary">
          Reportes médicos
        </p>
        <h1 className="text-4xl font-semibold tracking-tight">Resumen y exportación</h1>
        <p className="max-w-2xl text-lg leading-7 text-muted-foreground">
          Genera un PDF con datos reales para compartir con el médico o la familia.
        </p>
      </section>

      <ReportsDashboard />
    </main>
  );
}
