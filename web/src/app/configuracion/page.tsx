import type { Metadata } from "next";

import { PatientSettingsForm } from "@/components/patient/patient-settings-form";

export const metadata: Metadata = {
  title: "Configuración | CuidaVoz",
};

export default function SettingsPage() {
  return (
    <main className="mx-auto flex min-h-screen w-full max-w-2xl flex-col gap-6 bg-[#fbf7ef] px-4 py-6 text-slate-950 sm:px-6">
      <header className="space-y-3">
        <p className="inline-flex w-fit rounded-full bg-secondary px-4 py-2 text-sm font-medium text-secondary-foreground">
          Configuración
        </p>
        <h1 className="text-4xl font-semibold tracking-tight text-balance">
          Datos del paciente
        </h1>
        <p className="max-w-xl text-lg leading-7 text-muted-foreground">
          Actualiza la información básica que usa CuidaVoz para María.
        </p>
        <p className="max-w-xl text-base leading-7 text-muted-foreground">
          Estos datos se usarán en el panel familiar, reportes y recordatorios.
        </p>
      </header>

      <PatientSettingsForm />
    </main>
  );
}
