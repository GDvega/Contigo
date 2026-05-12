import { Image, StyleSheet, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useState } from "react";

import { AppCard } from "@/components/AppCard";
import { getAssetUrl } from "@/lib/mobileData";
import { colors } from "@/theme";
import type { MedicationGroup } from "@/types";
import { formatScheduleTime } from "@/utils/dates";
import { formatMedicationGroupTitle } from "@/utils/medications";

type MedicationReminderCardProps = {
  group: MedicationGroup | null;
  highlighted?: boolean;
};

export function MedicationReminderCard({
  group,
  highlighted = false,
}: MedicationReminderCardProps) {
  const [imageFailed, setImageFailed] = useState(false);

  if (!group) {
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

  const pendingMedications = group.medications.filter(
    (medication) => medication.statusToday === "PENDING"
  );
  const medicationsToShow = pendingMedications.length > 0 ? pendingMedications : group.medications;
  const medication = medicationsToShow[0];
  const imageUrl = getAssetUrl(medication.imageUri);
  const isGrouped = group.pendingMedications > 1;

  return (
    <AppCard tone={highlighted ? "red" : "amber"}>
      <Text style={styles.eyebrow}>{formatMedicationGroupTitle(group)}</Text>
      <View style={styles.row}>
        {imageUrl && !imageFailed ? (
          <Image
            source={{ uri: imageUrl }}
            style={styles.image}
            onError={() => setImageFailed(true)}
          />
        ) : (
          <View style={styles.fallback}>
            <Ionicons name="medkit" size={42} color={colors.primary} />
          </View>
        )}

        <View style={styles.details}>
          {isGrouped ? (
            <>
              <Text style={styles.name}>Tus pastillas</Text>
              <Text style={styles.time}>
                {group.pendingMedications} pastillas programadas para las{" "}
                {formatScheduleTime(group.scheduleTime)}
              </Text>
              <View style={styles.groupList}>
                {medicationsToShow.map((item) => (
                  <Text key={item.id} style={styles.groupItem}>
                    {item.name} · {item.dose}
                  </Text>
                ))}
              </View>
            </>
          ) : (
            <>
              <Text style={styles.name}>{medication.name}</Text>
              <Text style={styles.time}>{formatScheduleTime(medication.scheduleTime)}</Text>
              <Text style={styles.dose}>{medication.dose}</Text>
              <Text style={styles.text}>
                {[medication.color, medication.shape].filter(Boolean).join(", ")}
              </Text>
              {medication.instructions ? (
                <Text style={styles.instructions}>{medication.instructions}</Text>
              ) : null}
            </>
          )}
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
  groupList: {
    gap: 6,
    marginTop: 4,
  },
  groupItem: {
    color: colors.text,
    fontSize: 17,
    fontWeight: "800",
    lineHeight: 24,
  },
});
