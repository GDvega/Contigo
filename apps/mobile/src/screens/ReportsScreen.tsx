import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, StyleSheet, Text, View } from "react-native";

import { AppCard } from "@/components/AppCard";
import { Screen } from "@/components/Screen";
import { api } from "@/lib/api";
import { colors } from "@/theme";
import type { MedicalReportSummary } from "@/types";
import { formatDateTime } from "@/utils/dates";

export function ReportsScreen() {
  const [report, setReport] = useState<MedicalReportSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const loadReport = useCallback(async () => {
    setReport(await api.getMedicalReportSummary());
  }, []);

  useEffect(() => {
    loadReport().finally(() => setIsLoading(false));
  }, [loadReport]);

  async function refresh() {
    setIsRefreshing(true);
    try {
      await loadReport();
    } finally {
      setIsRefreshing(false);
    }
  }

  if (isLoading) {
    return (
      <Screen scroll={false}>
        <View style={styles.loading}>
          <ActivityIndicator color={colors.primary} size="large" />
          <Text style={styles.loadingText}>Cargando información...</Text>
        </View>
      </Screen>
    );
  }

  return (
    <Screen refreshing={isRefreshing} onRefresh={() => void refresh()}>
      <View>
        <Text style={styles.eyebrow}>Reportes</Text>
        <Text style={styles.title}>Resumen médico</Text>
        <Text style={styles.subtitle}>
          Consulta un resumen claro de presión, pulso y pastillas.
        </Text>
      </View>

      {report ? (
        <>
          <AppCard>
            <Text style={styles.sectionTitle}>{report.patient.fullName}</Text>
            <Text style={styles.bodyText}>
              Generado: {formatDateTime(report.generatedAt)}
            </Text>
          </AppCard>
          <AppCard>
            <Text style={styles.sectionTitle}>Lecturas</Text>
            <Text style={styles.metric}>
              {report.bloodPressure.readings.length}
            </Text>
            {report.bloodPressure.latestReading ? (
              <Text style={styles.bodyText}>
                Última: {report.bloodPressure.latestReading.systolic}/
                {report.bloodPressure.latestReading.diastolic}
              </Text>
            ) : null}
          </AppCard>
          <AppCard>
            <Text style={styles.sectionTitle}>Pastillas</Text>
            <Text style={styles.metric}>{report.medications.length}</Text>
            <Text style={styles.bodyText}>
              {report.medicationAdherence.takenLogsCount} tomas confirmadas
            </Text>
          </AppCard>
        </>
      ) : (
        <AppCard>
          <Text style={styles.bodyText}>No se pudo cargar el reporte.</Text>
        </AppCard>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  loading: {
    alignItems: "center",
    flex: 1,
    justifyContent: "center",
    gap: 14,
  },
  loadingText: {
    color: colors.text,
    fontSize: 20,
    fontWeight: "800",
  },
  eyebrow: {
    color: colors.primary,
    fontSize: 13,
    fontWeight: "900",
    letterSpacing: 1.2,
    textTransform: "uppercase",
  },
  title: {
    color: colors.text,
    fontSize: 32,
    fontWeight: "900",
    marginTop: 8,
  },
  subtitle: {
    color: colors.muted,
    fontSize: 17,
    lineHeight: 25,
    marginTop: 6,
  },
  sectionTitle: {
    color: colors.text,
    fontSize: 23,
    fontWeight: "900",
  },
  metric: {
    color: colors.primary,
    fontSize: 38,
    fontWeight: "900",
  },
  bodyText: {
    color: colors.muted,
    fontSize: 17,
    lineHeight: 25,
  },
});
