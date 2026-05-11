import { NextResponse } from "next/server";
import { ZodError } from "zod";

import {
  getPatientHealthSettings,
  updatePatientHealthSettings,
} from "@/features/patient-health-settings/patient-health-settings.service";
import { updatePatientHealthSettingsSchema } from "@/features/patient-health-settings/patient-health-settings.schema";

export async function GET() {
  try {
    const settings = await getPatientHealthSettings();

    return NextResponse.json({
      data: settings,
    });
  } catch {
    return NextResponse.json(
      {
        message: "No se pudieron cargar los rangos de salud.",
      },
      { status: 500 }
    );
  }
}

export async function PATCH(request: Request) {
  try {
    const body = await request.json();
    const data = updatePatientHealthSettingsSchema.parse(body);
    const settings = await updatePatientHealthSettings(data);

    return NextResponse.json({
      message: "Rangos de salud actualizados correctamente.",
      data: settings,
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
        message: "No se pudieron actualizar los rangos de salud.",
      },
      { status: 500 }
    );
  }
}
