import { NextResponse } from "next/server";
import { ZodError } from "zod";
import {
  createPressureReading,
  getPressureReadings,
  PressurePatientNotFoundError,
} from "@/features/blood-pressure/pressure.service";
import { createPressureReadingSchema } from "@/features/blood-pressure/pressure.schema";

export async function GET() {
  try {
    const readings = await getPressureReadings();

    return NextResponse.json({
      data: readings,
    });
  } catch {
    return NextResponse.json(
      {
        message: "No se pudo obtener el historial de presión arterial.",
      },
      { status: 500 }
    );
  }
}

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const data = createPressureReadingSchema.parse(body);

    const reading = await createPressureReading(data);

    return NextResponse.json(
      {
        message: "Presión arterial registrada correctamente.",
        data: reading,
      },
      { status: 201 }
    );
  } catch (error) {
    if (error instanceof ZodError) {
      return NextResponse.json(
        {
          message: "Datos inválidos.",
          errors: error.flatten().fieldErrors,
        },
        { status: 400 }
      );
    }

    if (error instanceof PressurePatientNotFoundError) {
      return NextResponse.json(
        {
          message: error.message,
        },
        { status: 404 }
      );
    }

    return NextResponse.json(
      {
        message: "No se pudo registrar la presión arterial.",
      },
      { status: 500 }
    );
  }
}
