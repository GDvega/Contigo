import { renderToBuffer } from "@react-pdf/renderer";
import { NextResponse } from "next/server";
import { createElement } from "react";

import { MedicalReportDocument } from "@/components/reports/MedicalReportDocument";
import { getMedicalReportData } from "@/features/reports/medical-report.service";

export const runtime = "nodejs";

export async function GET(request: Request) {
  try {
    const report = await getMedicalReportData();
    const { searchParams } = new URL(request.url);

    if (searchParams.get("format") === "pdf") {
      const document = createElement(MedicalReportDocument, {
        report,
      }) as Parameters<typeof renderToBuffer>[0];
      const buffer = await renderToBuffer(
        document
      );

      return new Response(new Uint8Array(buffer), {
        headers: {
          "Content-Type": "application/pdf",
          "Content-Disposition":
            'attachment; filename="reporte-medico-cuidavoz.pdf"',
        },
      });
    }

    return NextResponse.json({
      data: report,
    });
  } catch {
    return NextResponse.json(
      {
        message: "No se pudo generar el reporte médico.",
      },
      { status: 500 }
    );
  }
}
