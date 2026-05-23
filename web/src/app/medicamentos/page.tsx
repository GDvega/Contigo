import type { Metadata } from "next";

import { MedicationManagement } from "@/components/medication/medication-management";

export const metadata: Metadata = {
  title: "Medicamentos | CuidaVoz",
};

export default function MedicationsPage() {
  return (
    <main className="mx-auto flex min-h-screen w-full max-w-5xl flex-col gap-5 bg-[#fbf7ef] px-4 py-6 text-slate-950 sm:px-6">
      <section className="space-y-3">
        <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary">
          Gestión
        </p>
        <h1 className="text-4xl font-semibold tracking-tight text-balance">
          Configurar medicamentos
        </h1>
        <p className="max-w-2xl text-lg leading-7 text-muted-foreground">
          Administra horarios e identificación visual con un formulario simple.
        </p>
      </section>

      <MedicationManagement />
    </main>
  );
}
