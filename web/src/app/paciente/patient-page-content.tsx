"use client";

import { useEffect, useMemo, useState } from "react";

import { HelpDialogProvider } from "@/components/patient/help-dialog-provider";
import { PatientDailyStatusCard } from "@/components/patient/patient-daily-status-card";
import { PatientGreeting } from "@/components/patient/patient-greeting";
import { PatientMedicationPanel } from "@/components/patient/patient-medication-panel";
import type { DailyStatus } from "@/features/daily-status/daily-status.types";

type DailyStatusResponse = {
  data?: DailyStatus;
  error?: string;
  message?: string;
};

function getFirstName(fullName: string) {
  return fullName.trim().split(/\s+/)[0] ?? "";
}

export function PatientPageContent() {
  const [greetingState, setGreetingState] = useState<
    | { status: "loading"; fullName?: never }
    | { status: "success"; fullName: string }
    | { status: "error"; fullName?: never }
  >({
    status: "loading",
  });

  useEffect(() => {
    const controller = new AbortController();

    fetch("/api/daily-status", {
      cache: "no-store",
      signal: controller.signal,
    })
      .then(async (response) => {
        const payload = (await response.json()) as DailyStatusResponse;

        if (!response.ok || !payload.data?.patient.fullName) {
          throw new Error(payload.error ?? payload.message ?? "No patient");
        }

        setGreetingState({
          status: "success",
          fullName: payload.data.patient.fullName,
        });
      })
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") {
          return;
        }

        setGreetingState({
          status: "error",
        });
      });

    return () => controller.abort();
  }, []);

  const greeting = useMemo(() => {
    if (greetingState.status === "loading") {
      return "Buenos días";
    }

    if (greetingState.status === "error") {
      return "Hola";
    }

    const firstName = getFirstName(greetingState.fullName);

    return firstName ? `Buenos días, ${firstName}` : "Buenos días";
  }, [greetingState]);

  return (
    <HelpDialogProvider>
      <main className="mx-auto flex min-h-screen w-full max-w-xl flex-col gap-5 bg-[#fbf7ef] px-4 py-6 text-slate-950 sm:px-6">
        <PatientGreeting greeting={greeting} />
        <PatientMedicationPanel />
        <PatientDailyStatusCard />
      </main>
    </HelpDialogProvider>
  );
}
