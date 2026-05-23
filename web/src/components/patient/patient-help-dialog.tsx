"use client";

import { HeartHandshake } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

type PatientHelpDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

export function PatientHelpDialog({ open, onOpenChange }: PatientHelpDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg rounded-3xl p-6">
        <DialogHeader className="gap-3">
          <div className="flex h-16 w-16 items-center justify-center rounded-3xl bg-rose-100">
            <HeartHandshake className="size-8 text-rose-700" aria-hidden="true" />
          </div>
          <DialogTitle className="text-4xl font-semibold tracking-tight">
            ¿Necesitas ayuda?
          </DialogTitle>
          <DialogDescription className="text-xl leading-8">
            Puedes llamar a tu familiar de confianza.
          </DialogDescription>
        </DialogHeader>

        <div className="rounded-3xl border border-border bg-background px-5 py-5">
          <p className="text-lg font-semibold text-muted-foreground">Contacto familiar</p>
          <p className="mt-3 text-3xl font-semibold">Juan Rojas</p>
          <p className="mt-2 text-2xl font-semibold text-primary">+51 999 999 999</p>
        </div>

        <Button
          nativeButton={false}
          className="min-h-20 w-full rounded-3xl text-2xl font-semibold"
          render={<a href="tel:+51999999999" />}
        >
          Llamar ahora
        </Button>
      </DialogContent>
    </Dialog>
  );
}
