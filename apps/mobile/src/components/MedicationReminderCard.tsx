import { Image, StyleSheet, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";

import { AppCard } from "@/components/AppCard";
import { getAssetUrl } from "@/lib/api";
import { colors } from "@/theme";
import type { DailyMedication } from "@/types";

type MedicationReminderCardProps = {
  medication: DailyMedication | null;
};

export function MedicationReminderCard({
  medication,
}: MedicationReminderCardProps) {
  if (!medication) {
    return (
      <AppCard tone="green">
        <Text style={styles.successEyebrow}>Pastillas</Text>
        <Text style={styles.successTitle}>No hay pastillas pendientes</Text>
        <Text style={styles.successText}>
          Todas tus pastillas de hoy ya fueron registradas.
        </Text>
      </AppCard>
    );
  }

  const imageUrl = getAssetUrl(medication.imageUrl);

  return (
    <AppCard tone="amber">
      <Text style={styles.eyebrow}>Es hora de tomar tu pastilla</Text>
      <View style={styles.row}>
        {imageUrl ? (
          <Image source={{ uri: imageUrl }} style={styles.image} />
        ) : (
          <View style={styles.fallback}>
            <Ionicons name="medkit" size={42} color={colors.primary} />
          </View>
        )}

        <View style={styles.details}>
          <Text style={styles.name}>{medication.name}</Text>
          <Text style={styles.time}>{medication.scheduleTime}</Text>
          <Text style={styles.dose}>{medication.dose}</Text>
          <Text style={styles.text}>
            {[medication.color, medication.shape].filter(Boolean).join(", ")}
          </Text>
          {medication.instructions ? (
            <Text style={styles.instructions}>{medication.instructions}</Text>
          ) : null}
        </View>
      </View>
    </AppCard>
  );
}

const styles = StyleSheet.create({
  eyebrow: {
    color: colors.amberText,
    fontSize: 13,
    fontWeight: "900",
    letterSpacing: 1.3,
    textTransform: "uppercase",
  },
  row: {
    alignItems: "flex-start",
    flexDirection: "row",
    gap: 16,
  },
  image: {
    borderRadius: 26,
    height: 118,
    width: 118,
  },
  fallback: {
    alignItems: "center",
    backgroundColor: "#fff",
    borderRadius: 26,
    height: 118,
    justifyContent: "center",
    width: 118,
  },
  details: {
    flex: 1,
    gap: 5,
  },
  title: {
    color: colors.text,
    fontSize: 26,
    fontWeight: "900",
  },
  successEyebrow: {
    color: colors.greenText,
    fontSize: 12,
    fontWeight: "900",
    letterSpacing: 1,
    textTransform: "uppercase",
  },
  successTitle: {
    color: colors.text,
    fontSize: 24,
    fontWeight: "900",
  },
  successText: {
    color: colors.text,
    fontSize: 16,
    lineHeight: 22,
  },
  name: {
    color: colors.text,
    fontSize: 30,
    fontWeight: "900",
    lineHeight: 34,
  },
  time: {
    color: colors.text,
    fontSize: 24,
    fontWeight: "900",
  },
  dose: {
    color: colors.text,
    fontSize: 19,
    fontWeight: "800",
  },
  text: {
    color: colors.text,
    fontSize: 17,
    lineHeight: 23,
  },
  instructions: {
    color: colors.muted,
    fontSize: 16,
    lineHeight: 22,
  },
});
