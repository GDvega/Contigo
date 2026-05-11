import { HeartPulse } from "lucide-react";

type PatientGreetingProps = {
  greeting: string;
};

export function PatientGreeting({ greeting }: PatientGreetingProps) {
  return (
    <header className="space-y-3">
      <div className="inline-flex items-center gap-2 rounded-full bg-secondary px-4 py-2 text-sm font-medium text-secondary-foreground">
        <HeartPulse className="size-4" aria-hidden="true" />
        Modo paciente
      </div>
      <h1 className="max-w-sm text-4xl font-semibold tracking-tight text-balance sm:text-5xl">
        {greeting}
      </h1>
      <p className="max-w-md text-lg leading-7 text-muted-foreground sm:text-xl">
        Hoy te acompaño con tus pastillas y tu presión.
      </p>
    </header>
  );
}
