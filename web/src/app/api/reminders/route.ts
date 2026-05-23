import { NextResponse } from "next/server";

export async function GET() {
  return NextResponse.json({
    status: "not_configured",
    message: "Los recordatorios externos aún no están configurados.",
  });
}
