import { Mic, Volume2 } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";

export function VoicePromptCard() {
  return (
    <Card className="rounded-[2rem] border-none bg-[#0f6b6e] text-primary-foreground shadow-sm ring-0">
      <CardContent className="flex items-center justify-between gap-4 px-6 py-6">
        <div className="space-y-1">
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary-foreground/75">
        Voz
          </p>
          <h2 className="text-2xl font-semibold leading-tight sm:text-3xl">
            Toca y habla
          </h2>
          <p className="max-w-xs text-base leading-6 text-primary-foreground/85">
            Dime tu presión o pide ayuda sin escribir.
          </p>
        </div>
        <div className="flex h-18 w-18 items-center justify-center rounded-full bg-white/16">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-white text-primary">
            <Mic className="size-7" aria-hidden="true" />
          </div>
        </div>
      </CardContent>
      <div className="flex items-center gap-2 border-t border-white/20 px-6 py-4 text-sm text-primary-foreground/85">
        <Volume2 className="size-4" aria-hidden="true" />
        Asistente por voz para el control diario
      </div>
    </Card>
  );
}
