import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  Image,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useFocusEffect } from "@react-navigation/native";

import { MedicationEditorModal } from "@/components/MedicationEditorModal";
import { AppButton } from "@/components/AppButton";
import { AppCard } from "@/components/AppCard";
import { Screen } from "@/components/Screen";
import { deleteMedication, getAssetUrl, getDailyStatus, getMedications } from "@/lib/mobileData";
import { colors } from "@/theme";
import type { DailyStatus, Medication } from "@/types";

export function MedicationsScreen() {
  const [medications, setMedications] = useState<Medication[]>([]);
  const [dailyStatus, setDailyStatus] = useState<DailyStatus | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [hasError, setHasError] = useState(false);
  const [editorVisible, setEditorVisible] = useState(false);
  const [editingMedication, setEditingMedication] = useState<Medication | null>(null);

  const statusByMedicationId = useMemo(
    () =>
      new Map(
        (dailyStatus?.medications ?? []).map((medication) => [
          medication.id,
          medication.statusToday,
        ])
      ),
    [dailyStatus]
  );

  const loadMedications = useCallback(async () => {
    const [medicationsData, statusData] = await Promise.all([
      getMedications(),
      getDailyStatus(),
    ]);

    setMedications(medicationsData);
    setDailyStatus(statusData);
    setHasError(false);
  }, []);

  useEffect(() => {
    loadMedications()
      .catch(() => setHasError(true))
      .finally(() => setIsLoading(false));
  }, [loadMedications]);

  useFocusEffect(
    useCallback(() => {
      void loadMedications().catch(() => setHasError(true));
    }, [loadMedications])
  );

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

  function openCreateModal() {
    setEditingMedication(null);
    setEditorVisible(true);
  }

  function openEditModal(medication: Medication) {
    setEditingMedication(medication);
    setEditorVisible(true);
  }

  function closeEditor() {
    setEditorVisible(false);
    setEditingMedication(null);
  }

  function confirmDeleteMedication(medication: Medication) {
    Alert.alert(
      "Eliminar pastilla",
      "Esta pastilla dejará de aparecer en los recordatorios y en la lista principal.",
      [
        {
          text: "Cancelar",
          style: "cancel",
        },
        {
          text: "Eliminar",
          style: "destructive",
          onPress: () => {
            void handleDeleteMedication(medication.id);
          },
        },
      ]
    );
  }

  async function handleDeleteMedication(id: string) {
    try {
      await deleteMedication(id);
      await loadMedications();
    } catch {
      Alert.alert("CuidaVoz", "No pudimos guardar los datos en este dispositivo.");
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
    <>
      <Screen refreshing={isRefreshing} onRefresh={() => void refresh()}>
        <View>
          <Text style={styles.eyebrow}>Pastillas</Text>
          <Text style={styles.title}>Pastillas</Text>
          <Text style={styles.subtitle}>
            Administra tus medicamentos y horarios.
          </Text>
        </View>

        <AppButton
          label="+ Agregar pastilla"
          onPress={openCreateModal}
          icon={<Ionicons name="add-circle" size={24} color="#fff" />}
        />

        {medications.length === 0 ? (
          <AppCard>
            <Text style={styles.empty}>No hay pastillas registradas.</Text>
          </AppCard>
        ) : (
          medications.map((medication) => (
            <MedicationCard
              key={medication.id}
              medication={medication}
              statusToday={statusByMedicationId.get(medication.id) ?? null}
              onEdit={() => openEditModal(medication)}
              onDelete={() => confirmDeleteMedication(medication)}
            />
          ))
        )}
      </Screen>

      <MedicationEditorModal
        visible={editorVisible}
        medication={editingMedication}
        onClose={closeEditor}
        onSaved={loadMedications}
      />
    </>
  );
}

function MedicationCard({
  medication,
  statusToday,
  onEdit,
  onDelete,
}: {
  medication: Medication;
  statusToday: "TAKEN" | "PENDING" | null;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const [imageFailed, setImageFailed] = useState(false);
  const imageUri = getAssetUrl(medication.imageUri);
  const scheduleTime =
    medication.schedules.find((schedule) => schedule.isActive)?.time ??
    medication.schedules[0]?.time ??
    "Sin hora";

  return (
    <AppCard>
      <View style={styles.cardTop}>
        {imageUri && !imageFailed ? (
          <Image
            source={{ uri: imageUri }}
            style={styles.image}
            onError={() => setImageFailed(true)}
          />
        ) : (
          <View style={styles.fallback}>
            <Ionicons name="medkit" size={34} color={colors.primary} />
          </View>
        )}
        <View style={styles.medicationText}>
          <Text style={styles.medicationName}>{medication.name}</Text>
          <Text style={styles.medicationMeta}>
            {medication.dose} · {scheduleTime}
          </Text>
          <Text style={styles.medicationDetail}>
            {[medication.color, medication.shape].filter(Boolean).join(", ") ||
              "Sin color o forma"}
          </Text>
          {medication.instructions ? (
            <Text style={styles.medicationInstructions}>
              {medication.instructions}
            </Text>
          ) : null}
          {statusToday ? (
            <Text
              style={[
                styles.statusBadge,
                statusToday === "TAKEN" ? styles.statusTaken : styles.statusPending,
              ]}
            >
              {statusToday === "TAKEN" ? "Tomada hoy" : "Pendiente hoy"}
            </Text>
          ) : null}
        </View>
      </View>

      <View style={styles.actionsRow}>
        <AppButton label="Editar" variant="secondary" onPress={onEdit} />
        <AppButton label="Eliminar" variant="danger" onPress={onDelete} />
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
  cardTop: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 14,
  },
  image: {
    borderRadius: 24,
    height: 112,
    width: 112,
  },
  fallback: {
    alignItems: "center",
    backgroundColor: colors.primarySoft,
    borderRadius: 24,
    height: 112,
    justifyContent: "center",
    width: 112,
  },
  medicationText: {
    flex: 1,
    gap: 5,
  },
  medicationName: {
    color: colors.text,
    fontSize: 24,
    fontWeight: "900",
  },
  medicationMeta: {
    color: colors.primary,
    fontSize: 18,
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
  statusBadge: {
    alignSelf: "flex-start",
    borderRadius: 999,
    fontSize: 15,
    fontWeight: "900",
    overflow: "hidden",
    paddingHorizontal: 12,
    paddingVertical: 7,
  },
  statusTaken: {
    backgroundColor: colors.green,
    color: colors.greenText,
  },
  statusPending: {
    backgroundColor: colors.amber,
    color: colors.amberText,
  },
  actionsRow: {
    gap: 10,
  },
});
