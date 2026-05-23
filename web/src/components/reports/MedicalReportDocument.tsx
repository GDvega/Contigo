import {
  Document,
  Page,
  StyleSheet,
  Text,
  View,
} from "@react-pdf/renderer";

import type {
  MedicalReportData,
  ReportPressureStatus,
} from "@/features/reports/medical-report.service";

type MedicalReportDocumentProps = {
  report: MedicalReportData;
};

const dateTimeFormatter = new Intl.DateTimeFormat("es-PE", {
  dateStyle: "medium",
  timeStyle: "short",
});

const statusLabels: Record<ReportPressureStatus, string> = {
  NORMAL: "Normal",
  ELEVATED: "Elevada",
  HIGH: "Alta",
  CRITICAL: "Crítica",
};

const riskLabels = {
  low: "Bajo",
  medium: "Medio",
  high: "Alto",
};

const styles = StyleSheet.create({
  page: {
    padding: 32,
    fontFamily: "Helvetica",
    fontSize: 10,
    color: "#173b34",
    backgroundColor: "#ffffff",
  },
  title: {
    fontSize: 24,
    fontWeight: 700,
    marginBottom: 8,
  },
  subtitle: {
    color: "#5f716d",
    fontSize: 10,
    marginBottom: 18,
  },
  section: {
    border: "1 solid #d9e5df",
    borderRadius: 12,
    padding: 12,
    marginBottom: 12,
  },
  sectionTitle: {
    fontSize: 14,
    fontWeight: 700,
    marginBottom: 8,
  },
  grid: {
    flexDirection: "row",
    gap: 8,
    marginBottom: 8,
  },
  stat: {
    flex: 1,
    borderRadius: 10,
    backgroundColor: "#edf7f1",
    padding: 10,
  },
  statLabel: {
    color: "#5f716d",
    fontSize: 8,
    marginBottom: 4,
  },
  statValue: {
    fontSize: 16,
    fontWeight: 700,
  },
  row: {
    flexDirection: "row",
    borderBottom: "1 solid #e7eee9",
    paddingVertical: 6,
  },
  headerRow: {
    flexDirection: "row",
    borderBottom: "1 solid #b8cec5",
    paddingBottom: 6,
    marginBottom: 2,
  },
  cell: {
    flex: 1,
    paddingRight: 6,
  },
  cellWide: {
    flex: 1.5,
    paddingRight: 6,
  },
  headerCell: {
    color: "#45635c",
    fontSize: 8,
    fontWeight: 700,
    textTransform: "uppercase",
  },
  textMuted: {
    color: "#5f716d",
  },
  medicationCard: {
    borderRadius: 10,
    backgroundColor: "#f7fbf8",
    padding: 10,
    marginBottom: 8,
  },
  medicationName: {
    fontSize: 12,
    fontWeight: 700,
    marginBottom: 4,
  },
});

function formatDateTime(value: string) {
  return dateTimeFormatter.format(new Date(value));
}

function optionalText(value: string | number | null | undefined) {
  return value ?? "No registrado";
}

function formatRange(min: number | null | undefined, max: number | null | undefined) {
  if (min == null && max == null) {
    return "No configurado";
  }

  if (min != null && max != null) {
    return `${min}–${max}`;
  }

  if (min != null) {
    return `Desde ${min}`;
  }

  return `Hasta ${max}`;
}

export function MedicalReportDocument({ report }: MedicalReportDocumentProps) {
  const latestPressure = report.bloodPressure.latestReading;

  return (
    <Document
      title="Reporte médico CuidaVoz"
      author="CuidaVoz"
      subject="Resumen médico del paciente"
    >
      <Page size="A4" style={styles.page}>
        <Text style={styles.title}>Reporte médico CuidaVoz</Text>
        <Text style={styles.subtitle}>
          Generado el {formatDateTime(report.generatedAt)}
        </Text>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Paciente</Text>
          <Text>Nombre: {report.patient.fullName}</Text>
          <Text>Edad: {optionalText(report.patient.age)}</Text>
          <Text>Notas: {optionalText(report.patient.notes)}</Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Resumen de presión arterial</Text>
          <View style={styles.grid}>
            <View style={styles.stat}>
              <Text style={styles.statLabel}>Última lectura</Text>
              <Text style={styles.statValue}>
                {latestPressure
                  ? `${latestPressure.systolic}/${latestPressure.diastolic}`
                  : "Sin datos"}
              </Text>
            </View>
            <View style={styles.stat}>
              <Text style={styles.statLabel}>Normal</Text>
              <Text style={styles.statValue}>{report.bloodPressure.counts.NORMAL}</Text>
            </View>
            <View style={styles.stat}>
              <Text style={styles.statLabel}>Elevada</Text>
              <Text style={styles.statValue}>
                {report.bloodPressure.counts.ELEVATED}
              </Text>
            </View>
            <View style={styles.stat}>
              <Text style={styles.statLabel}>Alta / Crítica</Text>
              <Text style={styles.statValue}>
                {report.bloodPressure.counts.HIGH +
                  report.bloodPressure.counts.CRITICAL}
              </Text>
            </View>
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Rangos recomendados</Text>
          <Text>
            Sistólica normal:{" "}
            {formatRange(
              report.healthSettings?.systolicMinNormal,
              report.healthSettings?.systolicMaxNormal
            )}
          </Text>
          <Text>
            Diastólica normal:{" "}
            {formatRange(
              report.healthSettings?.diastolicMinNormal,
              report.healthSettings?.diastolicMaxNormal
            )}
          </Text>
          <Text>
            Pulso normal:{" "}
            {formatRange(
              report.healthSettings?.pulseMinNormal,
              report.healthSettings?.pulseMaxNormal
            )}
          </Text>
          <Text>
            Recomendación del médico:{" "}
            {optionalText(report.healthSettings?.doctorRecommendation)}
          </Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Lecturas de presión</Text>
          <View style={styles.headerRow}>
            <Text style={[styles.cellWide, styles.headerCell]}>Fecha</Text>
            <Text style={[styles.cell, styles.headerCell]}>Presión</Text>
            <Text style={[styles.cell, styles.headerCell]}>Pulso</Text>
            <Text style={[styles.cell, styles.headerCell]}>Estado</Text>
          </View>
          {report.bloodPressure.readings.length === 0 ? (
            <Text style={styles.textMuted}>No hay lecturas registradas.</Text>
          ) : (
            report.bloodPressure.readings.map((reading) => (
              <View key={reading.id} style={styles.row}>
                <Text style={styles.cellWide}>{formatDateTime(reading.measuredAt)}</Text>
                <Text style={styles.cell}>
                  {reading.systolic}/{reading.diastolic}
                </Text>
                <Text style={styles.cell}>
                  {reading.pulse ? `${reading.pulse} lpm` : "—"}
                </Text>
                <Text style={styles.cell}>{statusLabels[reading.status]}</Text>
              </View>
            ))
          )}
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Medicamentos</Text>
          {report.medications.length === 0 ? (
            <Text style={styles.textMuted}>No hay medicamentos registrados.</Text>
          ) : (
            report.medications.map((medication) => (
              <View key={medication.id} style={styles.medicationCard}>
                <Text style={styles.medicationName}>{medication.name}</Text>
                <Text>Dosis: {medication.dose}</Text>
                <Text>
                  Horario:{" "}
                  {medication.schedules.find((schedule) => schedule.isActive)?.time ??
                    medication.schedules[0]?.time ??
                    "Sin horario"}
                </Text>
                <Text>Instrucciones: {optionalText(medication.instructions)}</Text>
              </View>
            ))
          )}
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Adherencia a medicamentos</Text>
          <Text>Tomas confirmadas: {report.medicationAdherence.takenLogsCount}</Text>
          <View style={styles.headerRow}>
            <Text style={[styles.cellWide, styles.headerCell]}>Medicamento</Text>
            <Text style={[styles.cellWide, styles.headerCell]}>Programada</Text>
            <Text style={[styles.cellWide, styles.headerCell]}>Tomada</Text>
          </View>
          {report.medicationAdherence.latestLogs.slice(0, 10).map((log) => (
            <View key={log.id} style={styles.row}>
              <Text style={styles.cellWide}>{log.medication.name}</Text>
              <Text style={styles.cellWide}>{formatDateTime(log.scheduledFor)}</Text>
              <Text style={styles.cellWide}>
                {log.takenAt ? formatDateTime(log.takenAt) : "No registrado"}
              </Text>
            </View>
          ))}
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Estado diario</Text>
          <Text>Total de medicamentos: {report.dailyStatus.totalMedications}</Text>
          <Text>Medicamentos tomados: {report.dailyStatus.takenMedications}</Text>
          <Text>Pendientes: {report.dailyStatus.pendingMedications}</Text>
          <Text>Riesgo actual: {riskLabels[report.dailyStatus.riskLevel]}</Text>
        </View>
      </Page>
    </Document>
  );
}
