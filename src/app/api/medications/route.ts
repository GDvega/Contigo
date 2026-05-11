import { NextResponse } from "next/server";
import { ZodError } from "zod";

import {
  createMedication,
  getMedications,
} from "@/features/medications/medication.service";
import { createMedicationSchema } from "@/features/medications/medication.schema";

export async function GET() {
  try {
    const medications = await getMedications();

    return NextResponse.json({
      data: medications,
    });
  } catch {
    return NextResponse.json(
      {
        message: "No se pudo obtener la lista de medicamentos.",
      },
      { status: 500 }
    );
  }
}

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const data = createMedicationSchema.parse(body);

    const medication = await createMedication(data);

    return NextResponse.json(
      {
        message: "Medicamento registrado correctamente.",
        data: medication,
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

    return NextResponse.json(
      {
        message: "No se pudo registrar el medicamento.",
      },
      { status: 500 }
    );
  }
}
