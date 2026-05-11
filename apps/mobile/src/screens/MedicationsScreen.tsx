import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, Image, StyleSheet, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";

import { AppButton } from "@/components/AppButton";
import { AppCard } from "@/components/AppCard";
import { Screen } from "@/components/Screen";
import { api, getAssetUrl } from "@/lib/api";
import { colors } from "@/theme";
import type { Medication } from "@/types";

export function MedicationsScreen() {
  const [medications, setMedications] = useState<Medication[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [hasError, setHasError] = useState(false);

  const loadMedications = useCallback(async () => {
    const data = await api.getMedications();
    setMedications(data);
    setHasError(false);
  }, []);

  useEffect(() => {
    loadMedications()
      .catch(() => setHasError(true))
      .finally(() => setIsLoading(false));
  }, [loadMedications]);

  async function refresh() {
    setIsRefreshing(true);
    try {
      await loadMedications();
    } catch {
      setHasError(true);
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
        <Text style={styles.eyebrow}>Pastillas</Text>
        <Text style={styles.title}>Lista de pastillas</Text>
        <Text style={styles.subtitle}>
          Consulta horarios, imágenes e indicaciones configuradas por la familia.
        </Text>
      </View>

      {medications.length === 0 ? (
        <AppCard>
          <Text style={styles.empty}>No hay pastillas registradas.</Text>
        </AppCard>
      ) : (
        medications.map((medication) => (
          <MedicationCard key={medication.id} medication={medication} />
        ))
      )}
    </Screen>
  );
}

function MedicationCard({ medication }: { medication: Medication }) {
  const imageUrl = getAssetUrl(medication.imageUrl);
  const scheduleTime =
    medication.schedules.find((schedule) => schedule.isActive)?.time ??
    medication.schedules[0]?.time ??
    "Sin hora";

  return (
    <AppCard>
      <View style={styles.medicationRow}>
        {imageUrl ? (
          <Image source={{ uri: imageUrl }} style={styles.image} />
        ) : (
          <View style={styles.fallback}>
            <Ionicons name="medkit" size={30} color={colors.primary} />
          </View>
        )}
        <View style={styles.medicationText}>
          <Text style={styles.medicationName}>{medication.name}</Text>
          <Text style={styles.medicationMeta}>
            {medication.dose} · {scheduleTime}
          </Text>
          <Text style={styles.medicationDetail}>
            {[medication.color, medication.shape].filter(Boolean).join(", ")}
          </Text>
          {medication.instructions ? (
            <Text style={styles.medicationInstructions}>
              {medication.instructions}
            </Text>
          ) : null}
        </View>
      </View>
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
  empty: {
    color: colors.muted,
    fontSize: 18,
    fontWeight: "700",
  },
  medicationRow: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 14,
  },
  image: {
    borderRadius: 22,
    height: 82,
    width: 82,
  },
  fallback: {
    alignItems: "center",
    backgroundColor: colors.primarySoft,
    borderRadius: 22,
    height: 82,
    justifyContent: "center",
    width: 82,
  },
  medicationText: {
    flex: 1,
    gap: 4,
  },
  medicationName: {
    color: colors.text,
    fontSize: 23,
    fontWeight: "900",
  },
  medicationMeta: {
    color: colors.primary,
    fontSize: 17,
    fontWeight: "900",
  },
  medicationDetail: {
    color: colors.text,
    fontSize: 16,
    fontWeight: "700",
  },
  medicationInstructions: {
    color: colors.muted,
    fontSize: 15,
    lineHeight: 22,
  },
});
