import type { Metadata } from "next";

import "./globals.css";

export const metadata: Metadata = {
  title: "CuidaVoz",
  description: "Asistente de salud por voz para adultos mayores y sus familiares.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="es" className="h-full antialiased">
      <body className="min-h-full font-sans">{children}</body>
    </html>
  );
}
