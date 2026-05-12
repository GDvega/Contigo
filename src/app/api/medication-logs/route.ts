import { NextResponse } from "next/server";
import { ZodError } from "zod";

import {
  createMedicationLog,
  getMedicationLogs,
  MedicationLogMedicationNotFoundError,
} from "@/features/medications/medication-log.service";
import { createMedicationLogSchema } from "@/features/medications/medication-log.schema";

export async function GET() {
  try {
    const logs = await getMedicationLogs();

    return NextResponse.json({
      data: logs,
    });
  } catch {
    return NextResponse.json(
      {
        message: "No se pudo obtener el historial de tomas.",
      },
      { status: 500 }
    );
  }
}

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const data = createMedicationLogSchema.parse(body);

    const result = await createMedicationLog(data);

    return NextResponse.json(
      {
        message: result.duplicate
          ? "Esta toma ya fue registrada."
          : "Toma registrada correctamente.",
        data: result.medicationLog,
        duplicate: result.duplicate,
      },
      { status: result.duplicate ? 200 : 201 }
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

    if (error instanceof MedicationLogMedicationNotFoundError) {
      return NextResponse.json(
        {
          message: error.message,
        },
        { status: 404 }
      );
    }

    return NextResponse.json(
      {
        message: "No se pudo registrar la toma.",
      },
      { status: 500 }
    );
  }
}
