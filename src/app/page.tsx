import Link from "next/link";
import {
  ChevronRight,
  HeartPulse,
  Mic,
  NotebookText,
  Pill,
  Settings,
  Users,
} from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const routes = [
  {
    href: "/paciente",
    title: "Modo paciente",
    description: "Pantalla simple, grande y guiada por voz para María.",
    icon: Mic,
  },
  {
    href: "/familiar",
    title: "Panel familiar",
    description: "Resumen de presión, pastillas, rangos y alertas.",
    icon: Users,
  },
  {
    href: "/medicamentos",
    title: "Medicamentos",
    description: "Gestiona horarios, imágenes e instrucciones.",
    icon: Pill,
  },
  {
    href: "/historial",
    title: "Historial",
    description: "Consulta registros de presión y pulso.",
    icon: HeartPulse,
  },
  {
    href: "/reportes",
    title: "Reportes",
    description: "Genera reportes médicos en PDF.",
    icon: NotebookText,
  },
  {
    href: "/configuracion",
    title: "Configuración",
    description: "Edita datos del paciente y preferencias.",
    icon: Settings,
  },
];

export default function HomePage() {
  return (
    <main className="mx-auto flex min-h-screen w-full max-w-5xl flex-col gap-8 bg-[#fbf7ef] px-4 py-8 text-slate-950 sm:px-6">
      <section className="rounded-[2rem] bg-[#0f6b6e] px-6 py-10 text-primary-foreground shadow-sm sm:px-8">
        <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary-foreground/75">
          CuidaVoz
        </p>
        <h1 className="mt-3 max-w-2xl text-4xl font-semibold tracking-tight text-balance sm:text-5xl">
          Asistente de salud por voz para el cuidado diario de adultos mayores.
        </h1>
        <p className="mt-4 max-w-2xl text-lg leading-7 text-primary-foreground/85">
          Acompaña recordatorios de medicamentos, presión arterial, reportes y seguimiento familiar en un solo lugar.
        </p>
      </section>

      <section className="grid gap-4 md:grid-cols-2">
        {routes.map(({ href, title, description, icon: Icon }) => (
          <Link key={href} href={href} className="block">
            <Card className="h-full rounded-[2rem] border-none bg-white shadow-sm ring-1 ring-primary/10 transition-transform hover:-translate-y-0.5">
              <CardHeader className="space-y-3">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-secondary">
                  <Icon className="size-5 text-primary" aria-hidden="true" />
                </div>
                <CardTitle className="text-2xl">{title}</CardTitle>
              </CardHeader>
              <CardContent className="flex items-end justify-between gap-4 pb-6">
                <p className="max-w-sm text-base leading-7 text-muted-foreground">
                  {description}
                </p>
                <ChevronRight className="size-5 text-muted-foreground" aria-hidden="true" />
              </CardContent>
            </Card>
          </Link>
        ))}
      </section>
    </main>
  );
}
