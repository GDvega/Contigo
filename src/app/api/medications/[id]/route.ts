import { NextResponse } from "next/server";
import { ZodError } from "zod";

import {
  deleteMedication,
  updateMedication,
} from "@/features/medications/medication.service";
import { updateMedicationSchema } from "@/features/medications/medication.schema";

type MedicationRouteContext = {
  params: Promise<{
    id: string;
  }>;
};

export async function PATCH(request: Request, context: MedicationRouteContext) {
  try {
    const { id } = await context.params;
    const body = await request.json();
    const data = updateMedicationSchema.parse(body);
    const medication = await updateMedication(id, data);

    return NextResponse.json({
      message: "Medicamento actualizado correctamente.",
      data: medication,
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
        message: "No se pudo actualizar el medicamento.",
      },
      { status: 500 }
    );
  }
}

export async function DELETE(_request: Request, context: MedicationRouteContext) {
  try {
    const { id } = await context.params;
    const medication = await deleteMedication(id);

    return NextResponse.json({
      message: "Medicamento eliminado correctamente.",
      data: medication,
    });
  } catch {
    return NextResponse.json(
      {
        message: "No se pudo eliminar el medicamento.",
      },
      { status: 500 }
    );
  }
}
