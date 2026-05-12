import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, Alert, StyleSheet, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useFocusEffect } from "@react-navigation/native";

import { AppButton } from "@/components/AppButton";
import { AppCard } from "@/components/AppCard";
import { Screen } from "@/components/Screen";
import { getMedicalReportData, type MedicalReportData } from "@/lib/reportData";
import { generateMedicalReportPdf, shareMedicalReport } from "@/lib/pdfReport";
import { colors } from "@/theme";
import { formatDateTime } from "@/utils/dates";

type ReportsScreenProps = {
  onBack?: () => void;
};

export function ReportsScreen({ onBack }: ReportsScreenProps) {
  const [report, setReport] = useState<MedicalReportData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isSharing, setIsSharing] = useState(false);
  const [generatedFile, setGeneratedFile] = useState<{
    uri: string;
    filename: string;
  } | null>(null);

  const loadReport = useCallback(async () => {
    try {
      setReport(await getMedicalReportData());
    } catch (error) {
      if (
        error instanceof Error &&
        error.message === "No hay registros suficientes para generar el reporte."
      ) {
        setReport(null);
        return;
      }

      throw error;
    }
  }, []);

  useEffect(() => {
    loadReport().finally(() => setIsLoading(false));
  }, [loadReport]);

  useFocusEffect(
    useCallback(() => {
      void loadReport();
    }, [loadReport])
  );

  async function refresh() {
    setIsRefreshing(true);
    try {
      await loadReport();
    } finally {
      setIsRefreshing(false);
    }
  }

  async function handleGeneratePdf() {
    setIsGenerating(true);

    try {
      const pdf = await generateMedicalReportPdf();
      setGeneratedFile(pdf);
      await loadReport();
      Alert.alert("CuidaVoz", "Reporte generado correctamente.");
    } catch (error) {
      Alert.alert(
        "CuidaVoz",
        error instanceof Error &&
          error.message === "No hay registros suficientes para generar el reporte."
          ? error.message
          : "No se pudo generar el reporte. Intenta otra vez."
      );
    } finally {
      setIsGenerating(false);
    }
  }

  async function handleSharePdf() {
    if (!generatedFile) {
      Alert.alert("CuidaVoz", "Primero genera el reporte.");
      return;
    }

    setIsSharing(true);

    try {
      await shareMedicalReport(generatedFile.uri);
    } catch {
      Alert.alert("CuidaVoz", "No se pudo abrir el panel para compartir.");
    } finally {
      setIsSharing(false);
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
        <Text style={styles.title}>Reporte médico</Text>
        <Text style={styles.subtitle}>
          Genera un resumen para compartir con el médico.
        </Text>
      </View>

      {onBack ? (
        <AppButton
          label="Volver a Familia"
          variant="secondary"
          icon={<Ionicons name="arrow-back" size={22} color={colors.text} />}
          onPress={onBack}
        />
      ) : null}

      <AppCard tone="blue">
        <Text style={styles.sectionTitle}>Exportar resumen</Text>
        <Text style={styles.bodyText}>
          El PDF se genera en este celular y funciona sin internet.
        </Text>
        <AppButton
          label="Generar reporte PDF"
          loading={isGenerating}
          loadingLabel="Generando reporte..."
          onPress={() => void handleGeneratePdf()}
        />
        <AppButton
          label="Compartir reporte"
          variant="secondary"
          disabled={!generatedFile || isGenerating}
          loading={isSharing}
          loadingLabel="Abriendo..."
          onPress={() => void handleSharePdf()}
        />
        {!generatedFile ? (
          <Text style={styles.hint}>Primero genera el reporte.</Text>
        ) : (
          <Text style={styles.hint}>
            Último archivo: {generatedFile.filename}
          </Text>
        )}
      </AppCard>

      {report ? (
        <>
          <AppCard>
            <Text style={styles.sectionTitle}>{report.patient.fullName}</Text>
            <Text style={styles.bodyText}>
              Generado: {formatDateTime(report.generatedAt)}
            </Text>
            <Text style={styles.bodyText}>
              Riesgo actual: {report.summary.riskLevel === "low"
                ? "Bajo"
                : report.summary.riskLevel === "medium"
                  ? "Medio"
                  : "Alto"}
            </Text>
          </AppCard>
          <AppCard>
            <Text style={styles.sectionTitle}>Resumen rápido</Text>
            <Text style={styles.metric}>
              {report.bloodPressureHistory.length}
            </Text>
            <Text style={styles.bodyText}>lecturas registradas</Text>
            <Text style={styles.bodyText}>
              {report.medicationLogs.length} tomas en el historial reciente
            </Text>
          </AppCard>
        </>
      ) : (
        <AppCard>
          <Text style={styles.bodyText}>
            No hay registros suficientes para generar el reporte.
          </Text>
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
    color: colors.text,
    fontSize: 17,
    lineHeight: 25,
  },
  hint: {
    color: colors.muted,
    fontSize: 15,
    lineHeight: 22,
  },
});
