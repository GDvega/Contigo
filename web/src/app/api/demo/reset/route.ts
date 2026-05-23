import { NextResponse } from "next/server";

import { resetDemoData } from "@/features/demo/demo-reset.service";

function isDemoResetAllowed() {
  return process.env.NODE_ENV !== "production";
}

export async function POST() {
  if (!isDemoResetAllowed()) {
    return NextResponse.json(
      { error: "Demo reset is disabled in production." },
      { status: 403 }
    );
  }

  try {
    const data = await resetDemoData();

    return NextResponse.json({
      message: "Datos demo preparados correctamente.",
      data,
    });
  } catch (error) {
    console.error("[demo/reset]", error);
    return NextResponse.json(
      {
        message:
          error instanceof Error
            ? error.message
            : "No se pudieron preparar los datos demo.",
      },
      { status: 500 }
    );
  }
}
