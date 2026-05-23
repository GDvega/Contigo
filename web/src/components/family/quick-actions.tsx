import { Download, History, Settings2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const actions = [
  { label: "Ver historial", icon: History },
  { label: "Configurar medicamento", icon: Settings2 },
  { label: "Exportar reporte", icon: Download },
];

export function QuickActions() {
  return (
    <Card className="rounded-3xl border-none">
      <CardHeader>
        <CardTitle className="text-2xl">Acciones rápidas</CardTitle>
      </CardHeader>
      <CardContent className="grid gap-3 pb-6">
        {actions.map(({ label, icon: Icon }) => (
          <Button
            key={label}
            variant="outline"
            className="h-auto min-h-16 justify-start rounded-2xl px-4 py-4 text-lg font-semibold"
          >
            <Icon className="mr-3 size-5" aria-hidden="true" />
            {label}
          </Button>
        ))}
      </CardContent>
    </Card>
  );
}
