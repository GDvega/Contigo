import type { Metadata } from "next";

import { PatientPageContent } from "./patient-page-content";

export const metadata: Metadata = {
  title: "Paciente | CuidaVoz",
};

export default function PatientPage() {
  return <PatientPageContent />;
}
