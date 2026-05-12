import { NextResponse } from "next/server";

import {
  DAILY_STATUS_DEMO_NOT_FOUND_MESSAGE,
  DailyStatusPatientNotFoundError,
  getDailyStatus,
} from "@/features/daily-status/daily-status.service";

export async function GET() {
  try {
    const dailyStatus = await getDailyStatus();

    return NextResponse.json({
      data: dailyStatus,
    });
  } catch (error) {
    if (error instanceof DailyStatusPatientNotFoundError) {
      return NextResponse.json(
        {
          error: DAILY_STATUS_DEMO_NOT_FOUND_MESSAGE,
          message: DAILY_STATUS_DEMO_NOT_FOUND_MESSAGE,
        },
        { status: 404 }
      );
    }

    return NextResponse.json(
      {
        message: "No se pudo obtener el estado de hoy.",
      },
      { status: 500 }
    );
  }
}
