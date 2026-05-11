import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, StyleSheet, Text, View } from "react-native";
import { useFocusEffect } from "@react-navigation/native";

import { AppButton } from "@/components/AppButton";
import { AppCard } from "@/components/AppCard";
import { PressureStatusBadge } from "@/components/StatusBadge";
import { Screen } from "@/components/Screen";
import { api } from "@/lib/api";
import { colors } from "@/theme";
import type { BloodPressureReading } from "@/types";
import { formatDateTime } from "@/utils/dates";

export function HistoryScreen() {
  const [readings, setReadings] = useState<BloodPressureReading[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [hasError, setHasError] = useState(false);
  const [refreshError, setRefreshError] = useState<string | null>(null);

  const loadReadings = useCallback(async () => {
    const data = await api.getBloodPressureReadings();
    setReadings(
      [...data].sort(
        (first, second) =>
          new Date(second.measuredAt).getTime() -
          new Date(first.measuredAt).getTime()
      )
    );
    setHasError(false);
    setRefreshError(null);
  }, []);

  useEffect(() => {
    loadReadings()
      .catch(() => setHasError(true))
      .finally(() => setIsLoading(false));
  }, [loadReadings]);

  useFocusEffect(
    useCallback(() => {
      void loadReadings().catch(() => {
        setRefreshError("No pudimos actualizar los datos.");
      });
    }, [loadReadings])
  );

  async function refresh() {
    setIsRefreshing(true);
    try {
      await loadReadings();
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

  if (hasError) {
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

  return (
    <Screen refreshing={isRefreshing} onRefresh={() => void refresh()}>
      <View>
        <Text style={styles.eyebrow}>Historial</Text>
        <Text style={styles.title}>Presión y pulso</Text>
        <Text style={styles.subtitle}>
          Revisa las mediciones registradas para María.
        </Text>
      </View>

      {refreshError ? <Text style={styles.refreshError}>{refreshError}</Text> : null}

      {readings.length === 0 ? (
        <AppCard>
          <Text style={styles.empty}>Aún no hay lecturas registradas.</Text>
        </AppCard>
      ) : (
        readings.map((reading) => (
          <AppCard key={reading.id}>
            <View style={styles.readingHeader}>
              <View>
                <Text style={styles.pressure}>
                  {reading.systolic}/{reading.diastolic}
                </Text>
                <Text style={styles.date}>{formatDateTime(reading.measuredAt)}</Text>
              </View>
              <PressureStatusBadge status={reading.status} />
            </View>
            {reading.pulse ? (
              <Text style={styles.pulse}>Pulso: {reading.pulse} lpm</Text>
            ) : null}
            {reading.notes ? <Text style={styles.notes}>{reading.notes}</Text> : null}
          </AppCard>
        ))
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
  empty: {
    color: colors.muted,
    fontSize: 18,
    fontWeight: "700",
  },
  refreshError: {
    backgroundColor: colors.red,
    borderRadius: 18,
    color: colors.redText,
    fontSize: 16,
    fontWeight: "900",
    padding: 14,
  },
  readingHeader: {
    alignItems: "flex-start",
    flexDirection: "row",
    justifyContent: "space-between",
    gap: 12,
  },
  pressure: {
    color: colors.text,
    fontSize: 34,
    fontWeight: "900",
  },
  date: {
    color: colors.muted,
    fontSize: 15,
    marginTop: 4,
  },
  pulse: {
    color: colors.text,
    fontSize: 17,
    fontWeight: "800",
  },
  notes: {
    color: colors.muted,
    fontSize: 15,
    lineHeight: 22,
  },
});
