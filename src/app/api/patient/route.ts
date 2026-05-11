import { NextResponse } from "next/server";
import { ZodError } from "zod";

import {
  getCurrentPatient,
  updateCurrentPatient,
} from "@/features/patient/patient.service";
import { updatePatientSchema } from "@/features/patient/patient.schema";

export async function GET() {
  try {
    const patient = await getCurrentPatient();

    if (!patient) {
      return NextResponse.json(
        {
          message: "No se encontró el paciente.",
        },
        { status: 404 }
      );
    }

    return NextResponse.json({
      data: patient,
    });
  } catch {
    return NextResponse.json(
      {
        message: "No se pudo obtener el paciente.",
      },
      { status: 500 }
    );
  }
}

export async function PATCH(request: Request) {
  try {
    const body = await request.json();
    const data = updatePatientSchema.parse(body);
    const patient = await updateCurrentPatient(data);

    return NextResponse.json({
      message: "Paciente actualizado correctamente.",
      data: patient,
    });
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
        message: "No se pudo actualizar el paciente.",
      },
      { status: 500 }
    );
  }
}
