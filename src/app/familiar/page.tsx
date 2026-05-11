import type { Metadata } from "next";

import { FamilyDashboard } from "@/components/family/family-dashboard";

export const metadata: Metadata = {
  title: "Familiar | CuidaVoz",
};

export default function FamilyPage() {
  return (
    <main className="mx-auto flex min-h-screen w-full max-w-5xl flex-col gap-5 bg-[#fbf7ef] px-4 py-6 text-slate-950 sm:px-6">
      <FamilyDashboard />
    </main>
  );
}
