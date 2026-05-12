import * as FileSystem from "expo-file-system/legacy";
import * as Print from "expo-print";
import * as Sharing from "expo-sharing";

import { getMedicalReportData, type MedicalReportData } from "@/lib/reportData";
import { formatDateTime } from "@/utils/dates";

function escapeHtml(value: string | number | null | undefined) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function riskLabel(value: MedicalReportData["summary"]["riskLevel"]) {
  if (value === "low") return "Bajo";
  if (value === "medium") return "Medio";
  return "Alto";
}

function pressureStatusLabel(value: string) {
  if (value === "NORMAL") return "Normal";
  if (value === "ELEVATED") return "Elevada";
  if (value === "HIGH") return "Alta";
  if (value === "CRITICAL") return "Crítica";
  return value;
}

function personalizedStatusLabel(value: string | null | undefined) {
  if (value === "within_range") return "Dentro de rango";
  if (value === "out_of_range") return "Fuera de rango";
  return "Sin configurar";
}

function medicationLogStatusLabel(value: string) {
  if (value === "TAKEN") return "Tomado";
  if (value === "MISSED") return "Omitido";
  return "Pendiente";
}

function toSafeSlug(text: string) {
  return text
    .toLowerCase()
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

function reportFilename(data: MedicalReportData) {
  const date = data.generatedAt.slice(0, 10);
  return `cuida-voz-reporte-${toSafeSlug(data.patient.fullName)}-${date}.pdf`;
}

function renderPressureHistoryRows(data: MedicalReportData) {
  return data.bloodPressureHistory
    .slice(0, 10)
    .map(
      (reading) => `
        <tr>
          <td>${escapeHtml(formatDateTime(reading.measuredAt))}</td>
          <td>${escapeHtml(`${reading.systolic}/${reading.diastolic}`)}</td>
          <td>${escapeHtml(reading.pulse ? `${reading.pulse}` : "-")}</td>
          <td>${escapeHtml(pressureStatusLabel(reading.status))}</td>
          <td>${escapeHtml(
            personalizedStatusLabel(reading.personalizedStatus)
          )}</td>
        </tr>
      `
    )
    .join("");
}

function renderMedicationRows(data: MedicalReportData) {
  return data.medications
    .map((medication) => {
      const schedule =
        medication.schedules.find((item) => item.isActive)?.time ??
        medication.schedules[0]?.time ??
        "-";

      return `
        <tr>
          <td>${escapeHtml(medication.name)}</td>
          <td>${escapeHtml(medication.dose)}</td>
          <td>${escapeHtml(schedule)}</td>
          <td>${escapeHtml(medication.color ?? "-")}</td>
          <td>${escapeHtml(medication.shape ?? "-")}</td>
          <td>${escapeHtml(medication.instructions ?? "-")}</td>
        </tr>
      `;
    })
    .join("");
}

function renderMedicationLogRows(data: MedicalReportData) {
  return data.medicationLogs
    .map(
      (log) => `
        <tr>
          <td>${escapeHtml(log.medicationName)}</td>
          <td>${escapeHtml(
            log.scheduledFor ? formatDateTime(log.scheduledFor) : "-"
          )}</td>
          <td>${escapeHtml(log.takenAt ? formatDateTime(log.takenAt) : "-")}</td>
          <td>${escapeHtml(medicationLogStatusLabel(log.status))}</td>
        </tr>
      `
    )
    .join("");
}

export function generateMedicalReportHtml(data: MedicalReportData) {
  const latestPressure = data.latestPressure
    ? `${data.latestPressure.systolic}/${data.latestPressure.diastolic}`
    : "Sin registro";
  const latestPulse =
    data.latestPressure?.pulse !== null && data.latestPressure?.pulse !== undefined
      ? `${data.latestPressure.pulse} lpm`
      : "Sin registro";

  return `
    <html>
      <head>
        <meta charset="utf-8" />
        <style>
          body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
            background: #ffffff;
            color: #14213d;
            font-size: 12px;
            line-height: 1.5;
            padding: 24px;
          }
          h1, h2, h3 {
            color: #0f6b6e;
            margin: 0 0 10px;
          }
          h1 {
            font-size: 24px;
          }
          h2 {
            font-size: 18px;
            margin-top: 22px;
          }
          p {
            margin: 0 0 8px;
          }
          .card {
            border: 1px solid #d9e2ec;
            border-radius: 16px;
            padding: 16px;
            margin-top: 14px;
            background: #f8fbfb;
          }
          .grid {
            display: table;
            width: 100%;
            border-spacing: 10px 10px;
            margin: 0 -10px;
          }
          .metric {
            display: table-cell;
            width: 50%;
            border: 1px solid #d9e2ec;
            border-radius: 14px;
            padding: 14px;
            background: #ffffff;
            vertical-align: top;
          }
          .label {
            color: #486581;
            font-size: 11px;
            text-transform: uppercase;
            letter-spacing: 0.08em;
            font-weight: 700;
          }
          .value {
            font-size: 20px;
            font-weight: 700;
            margin-top: 6px;
          }
          table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 10px;
            background: #ffffff;
          }
          th, td {
            border: 1px solid #d9e2ec;
            padding: 8px;
            text-align: left;
            vertical-align: top;
          }
          th {
            background: #e7f3f1;
            color: #0f6b6e;
          }
          .disclaimer {
            font-size: 11px;
            color: #667085;
          }
        </style>
      </head>
      <body>
        <h1>Reporte médico CuidaVoz</h1>
        <p>Generado: ${escapeHtml(formatDateTime(data.generatedAt))}</p>
        <p class="disclaimer">
          CuidaVoz es una herramienta de apoyo. No reemplaza la evaluación de un profesional de salud.
        </p>

        <div class="card">
          <h2>Datos del paciente</h2>
          <p><strong>Nombre:</strong> ${escapeHtml(data.patient.fullName)}</p>
          <p><strong>Edad:</strong> ${escapeHtml(data.patient.age ?? "-")}</p>
          <p><strong>Notas:</strong> ${escapeHtml(data.patient.notes ?? "-")}</p>
        </div>

        <div class="card">
          <h2>Resumen diario</h2>
          <div class="grid">
            <div class="metric">
              <div class="label">Riesgo</div>
              <div class="value">${escapeHtml(riskLabel(data.summary.riskLevel))}</div>
            </div>
            <div class="metric">
              <div class="label">Pastillas</div>
              <div class="value">${escapeHtml(
                `${data.summary.takenMedications}/${data.summary.totalMedications}`
              )}</div>
              <p>Tomadas / totales</p>
            </div>
          </div>
          <p><strong>Pastillas pendientes:</strong> ${escapeHtml(
            data.summary.pendingMedications
          )}</p>
          <p><strong>Última presión:</strong> ${escapeHtml(latestPressure)}</p>
          <p><strong>Último pulso:</strong> ${escapeHtml(latestPulse)}</p>
        </div>

        <div class="card">
          <h2>Rangos recomendados</h2>
          <p><strong>Rango sistólica:</strong> ${escapeHtml(
            `${data.healthSettings?.systolicMinNormal ?? "-"} - ${
              data.healthSettings?.systolicMaxNormal ?? "-"
            }`
          )}</p>
          <p><strong>Rango diastólica:</strong> ${escapeHtml(
            `${data.healthSettings?.diastolicMinNormal ?? "-"} - ${
              data.healthSettings?.diastolicMaxNormal ?? "-"
            }`
          )}</p>
          <p><strong>Rango pulso:</strong> ${escapeHtml(
            `${data.healthSettings?.pulseMinNormal ?? "-"} - ${
              data.healthSettings?.pulseMaxNormal ?? "-"
            }`
          )}</p>
          <p><strong>Recomendación médica:</strong> ${escapeHtml(
            data.healthSettings?.doctorRecommendation ?? "-"
          )}</p>
        </div>

        <div class="card">
          <h2>Historial de presión</h2>
          <table>
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Presión</th>
                <th>Pulso</th>
                <th>Estado</th>
                <th>Rango personal</th>
              </tr>
            </thead>
            <tbody>
              ${renderPressureHistoryRows(data) || `
                <tr><td colspan="5">Sin registros suficientes.</td></tr>
              `}
            </tbody>
          </table>
        </div>

        <div class="card">
          <h2>Lista de pastillas</h2>
          <table>
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Dosis</th>
                <th>Horario</th>
                <th>Color</th>
                <th>Forma</th>
                <th>Instrucciones</th>
              </tr>
            </thead>
            <tbody>
              ${renderMedicationRows(data) || `
                <tr><td colspan="6">No hay pastillas activas.</td></tr>
              `}
            </tbody>
          </table>
        </div>

        <div class="card">
          <h2>Historial de tomas</h2>
          <table>
            <thead>
              <tr>
                <th>Medicamento</th>
                <th>Horario programado</th>
                <th>Hora registrada</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              ${renderMedicationLogRows(data) || `
                <tr><td colspan="4">No hay tomas registradas.</td></tr>
              `}
            </tbody>
          </table>
        </div>
      </body>
    </html>
  `;
}

export async function generateMedicalReportPdf() {
  const data = await getMedicalReportData();
  const html = generateMedicalReportHtml(data);
  const result = await Print.printToFileAsync({
    html,
    width: 794,
    height: 1123,
    base64: false,
  });

  const filename = reportFilename(data);
  const reportsDir = `${FileSystem.documentDirectory}reports/`;
  const info = await FileSystem.getInfoAsync(reportsDir);

  if (!info.exists) {
    await FileSystem.makeDirectoryAsync(reportsDir, { intermediates: true });
  }

  const destination = `${reportsDir}${filename}`;
  const existing = await FileSystem.getInfoAsync(destination);

  if (existing.exists) {
    await FileSystem.deleteAsync(destination, { idempotent: true });
  }

  await FileSystem.copyAsync({
    from: result.uri,
    to: destination,
  });

  return {
    uri: destination,
    filename,
  };
}

export async function shareMedicalReport(uri: string) {
  const available = await Sharing.isAvailableAsync();

  if (!available) {
    throw new Error("sharing_unavailable");
  }

  await Sharing.shareAsync(uri, {
    mimeType: "application/pdf",
    dialogTitle: "Compartir reporte médico",
    UTI: "com.adobe.pdf",
  });
}
