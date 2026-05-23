"use client";

import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";

import { PatientHelpDialog } from "@/components/patient/patient-help-dialog";

type HelpDialogContextValue = {
  openHelp: () => void;
  closeHelp: () => void;
};

const HelpDialogContext = createContext<HelpDialogContextValue | null>(null);

export function HelpDialogProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false);
  const openHelp = useCallback(() => setOpen(true), []);
  const closeHelp = useCallback(() => setOpen(false), []);

  const value = useMemo(
    () => ({
      openHelp,
      closeHelp,
    }),
    [openHelp, closeHelp]
  );

  return (
    <HelpDialogContext.Provider value={value}>
      {children}
      <PatientHelpDialog open={open} onOpenChange={setOpen} />
    </HelpDialogContext.Provider>
  );
}

export function useHelpDialog() {
  const ctx = useContext(HelpDialogContext);
  if (!ctx) {
    throw new Error("useHelpDialog must be used within HelpDialogProvider");
  }
  return ctx;
}
