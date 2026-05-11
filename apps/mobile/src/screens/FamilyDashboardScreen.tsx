import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, StyleSheet, Text, View } from "react-native";
import { useFocusEffect } from "@react-navigation/native";

import { AppButton } from "@/components/AppButton";
import { AppCard } from "@/components/AppCard";
import { PressureStatusBadge, RiskBadge } from "@/components/StatusBadge";
import { Screen } from "@/components/Screen";
import { api } from "@/lib/api";
import { colors } from "@/theme";
import type { DailyStatus } from "@/types";
import { formatDateTime } from "@/utils/dates";

export function FamilyDashboardScreen() {
  const [dailyStatus, setDailyStatus] = useState<DailyStatus | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [hasError, setHasError] = useState(false);
  const [refreshError, setRefreshError] = useState<string | null>(null);

  const loadDailyStatus = useCallback(async () => {
    const data = await api.getDailyStatus();
    setDailyStatus(data);
    setHasError(false);
    setRefreshError(null);
  }, []);

  useEffect(() => {
    loadDailyStatus()
      .catch(() => setHasError(true))
      .finally(() => setIsLoading(false));
  }, [loadDailyStatus]);

  useFocusEffect(
    useCallback(() => {
      void loadDailyStatus().catch(() => {
        setRefreshError("No pudimos actualizar los datos.");
      });
    }, [loadDailyStatus])
  );

  async function refresh() {
    setIsRefreshing(true);
    try {
      await loadDailyStatus();
    } catch {
      setRefreshError("No pudimos actualizar los datos.");
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

  if (hasError || !dailyStatus) {
    return (
      <Screen>
        <AppCard>
          <Text style={styles.errorTitle}>No pudimos cargar los datos.</Text>
          <Text style={styles.subtitle}>Intenta nuevamente en unos segundos.</Text>
          <AppButton label="Reintentar" onPress={() => void refresh()} />
        </AppCard>
      </Screen>
    );
  }

  const latestPressure = dailyStatus.latestPressure;
  const pending = dailyStatus.medications.filter(
    (medication) => medication.statusToday === "PENDING"
  );
  const hasHighPressure =
    latestPressure?.status === "HIGH" || latestPressure?.status === "CRITICAL";
  const isOutsideRecommendedRange =
    latestPressure?.personalizedStatus === "out_of_range";

  return (
    <Screen refreshing={isRefreshing} onRefresh={() => void refresh()}>
      <View>
        <Text style={styles.eyebrow}>Familia</Text>
        <Text style={styles.title}>Panel familiar</Text>
        <Text style={styles.subtitle}>Seguimiento diario de María Rojas.</Text>
      </View>

      {refreshError ? <Text style={styles.refreshError}>{refreshError}</Text> : null}

      <View style={styles.grid}>
        <MetricCard
          label="Estado general"
          value={
            dailyStatus.summary.riskLevel === "low"
              ? "Riesgo bajo"
              : dailyStatus.summary.riskLevel === "medium"
                ? "Riesgo medio"
                : "Riesgo alto"
          }
        >
          <RiskBadge risk={dailyStatus.summary.riskLevel} />
        </MetricCard>
        <MetricCard
          label="Pastillas"
          value={`${dailyStatus.summary.takenMedications} tomado${
            dailyStatus.summary.takenMedications === 1 ? "" : "s"
          }`}
          detail={`${dailyStatus.summary.pendingMedications} pendientes`}
        />
      </View>

      {pending.length > 0 ? (
        <AppCard tone="amber">
          <Text style={styles.sectionTitle}>Hay pastillas pendientes</Text>
          <Text style={styles.bodyText}>
            {pending
              .map((medication) => `${medication.name} · ${medication.scheduleTime}`)
              .join("\n")}
          </Text>
        </AppCard>
      ) : null}

      {hasHighPressure && latestPressure ? (
        <AppCard tone="red">
          <Text style={styles.sectionTitle}>Presión alta detectada</Text>
          <Text style={styles.bodyText}>
            {latestPressure.systolic}/{latestPressure.diastolic} ·{" "}
            {formatDateTime(latestPressure.measuredAt)}
          </Text>
        </AppCard>
      ) : null}

      {isOutsideRecommendedRange ? (
        <AppCard tone="blue">
          <Text style={styles.sectionTitle}>Revisar presión registrada</Text>
          <Text style={styles.bodyText}>
            La última lectura está fuera del rango recomendado por el médico.
          </Text>
        </AppCard>
      ) : null}

      {pending.length === 0 && !hasHighPressure && !isOutsideRecommendedRange ? (
        <AppCard tone="green">
          <Text style={styles.sectionTitle}>Todo en orden por ahora</Text>
          <Text style={styles.bodyText}>
            No hay alertas importantes en el estado diario.
          </Text>
        </AppCard>
      ) : null}

      <AppCard>
        <Text style={styles.sectionTitle}>Última presión</Text>
        {latestPressure ? (
          <>
            <View style={styles.pressureHeader}>
              <Text style={styles.pressure}>
                {latestPressure.systolic}/{latestPressure.diastolic}
              </Text>
              <PressureStatusBadge status={latestPressure.status} />
            </View>
            <Text style={styles.bodyText}>
              {latestPressure.pulse
                ? `Pulso: ${latestPressure.pulse} lpm · `
                : ""}
              {formatDateTime(latestPressure.measuredAt)}
            </Text>
            {latestPressure.personalizedStatus === "out_of_range" ? (
              <Text style={styles.warning}>
                Fuera del rango recomendado por el médico.
              </Text>
            ) : null}
          </>
        ) : (
          <Text style={styles.bodyText}>Sin lecturas registradas.</Text>
        )}
      </AppCard>

      <AppCard>
        <Text style={styles.sectionTitle}>Pastillas de hoy</Text>
        {dailyStatus.medications.map((medication) => (
          <View key={medication.id} style={styles.medicationRow}>
            <Text style={styles.medicationName}>{medication.name}</Text>
            <Text style={styles.medicationStatus}>
              {medication.statusToday === "TAKEN" ? "Tomada" : "Pendiente"}
            </Text>
          </View>
        ))}
      </AppCard>
    </Screen>
  );
}

function MetricCard({
  label,
  value,
  detail,
  children,
}: {
  label: string;
  value: string;
  detail?: string;
  children?: React.ReactNode;
}) {
  return (
    <AppCard>
      <Text style={styles.metricLabel}>{label}</Text>
      <Text style={styles.metricValue}>{value}</Text>
      {detail ? <Text style={styles.metricDetail}>{detail}</Text> : null}
      {children}
    </AppCard>
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
  errorTitle: {
    color: colors.text,
    fontSize: 26,
    fontWeight: "900",
  },
  subtitle: {
    color: colors.muted,
    fontSize: 17,
    lineHeight: 25,
    marginTop: 6,
  },
  refreshError: {
    backgroundColor: colors.red,
    borderRadius: 18,
    color: colors.redText,
    fontSize: 16,
    fontWeight: "900",
    padding: 14,
  },
  grid: {
    flexDirection: "row",
    gap: 12,
  },
  metricLabel: {
    color: colors.muted,
    fontSize: 13,
    fontWeight: "900",
    letterSpacing: 1,
    textTransform: "uppercase",
  },
  metricValue: {
    color: colors.text,
    fontSize: 22,
    fontWeight: "900",
  },
  metricDetail: {
    color: colors.muted,
    fontSize: 15,
    fontWeight: "700",
  },
  sectionTitle: {
    color: colors.text,
    fontSize: 23,
    fontWeight: "900",
  },
  bodyText: {
    color: colors.text,
    fontSize: 17,
    lineHeight: 25,
  },
  pressure: {
    color: colors.text,
    fontSize: 38,
    fontWeight: "900",
  },
  pressureHeader: {
    alignItems: "flex-start",
    flexDirection: "row",
    justifyContent: "space-between",
    gap: 12,
  },
  warning: {
    color: colors.blueText,
    fontSize: 16,
    fontWeight: "800",
  },
  medicationRow: {
    alignItems: "center",
    borderTopColor: colors.border,
    borderTopWidth: 1,
    flexDirection: "row",
    justifyContent: "space-between",
    paddingTop: 12,
  },
  medicationName: {
    color: colors.text,
    fontSize: 17,
    fontWeight: "800",
  },
  medicationStatus: {
    color: colors.primary,
    fontSize: 16,
    fontWeight: "900",
  },
});
