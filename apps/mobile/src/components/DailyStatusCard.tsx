import { useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";

import { AppCard } from "@/components/AppCard";
import { RiskBadge } from "@/components/StatusBadge";
import { colors } from "@/theme";
import type { DailyStatus } from "@/types";
import { formatDateTime } from "@/utils/dates";

type DailyStatusCardProps = {
  dailyStatus: DailyStatus;
};

const riskText = {
  low: "Todo en orden",
  medium: "Revisa tus pendientes",
  high: "Atención: revisa tu presión",
};

export function DailyStatusCard({ dailyStatus }: DailyStatusCardProps) {
  const [expanded, setExpanded] = useState(false);
  const pressure = dailyStatus.latestPressure;

  return (
    <AppCard>
      <View style={styles.header}>
        <View style={styles.headerText}>
          <Text style={styles.eyebrow}>Estado de hoy</Text>
          <Text style={styles.title}>{riskText[dailyStatus.summary.riskLevel]}</Text>
        </View>
        <RiskBadge risk={dailyStatus.summary.riskLevel} />
      </View>

      <Pressable
        accessibilityRole="button"
        accessibilityState={{ expanded }}
        onPress={() => setExpanded((current) => !current)}
        style={styles.expandButton}
      >
        <Text style={styles.expandText}>
          {expanded ? "Ocultar detalles" : "Ver detalles"}
        </Text>
        <Ionicons
          name={expanded ? "chevron-up" : "chevron-down"}
          size={22}
          color={colors.primary}
        />
      </Pressable>

      {expanded ? (
        <View style={styles.details}>
          <StatusRow
            label="Pastillas"
            value={
              dailyStatus.summary.pendingMedications > 0
                ? `${dailyStatus.summary.pendingMedications} pendiente${
                    dailyStatus.summary.pendingMedications === 1 ? "" : "s"
                  }`
                : "Todas tomadas"
            }
          />
          <StatusRow
            label="Presión"
            value={
              dailyStatus.summary.hasPressureReadingToday && pressure
                ? `Registrada ${pressure.systolic}/${pressure.diastolic}`
                : "Pendiente de registrar"
            }
          />
          {pressure?.pulse ? (
            <StatusRow label="Pulso" value={`${pressure.pulse} lpm`} />
          ) : null}
          {pressure ? (
            <Text style={styles.muted}>{formatDateTime(pressure.measuredAt)}</Text>
          ) : null}
          {pressure?.personalizedStatus === "out_of_range" ? (
            <View style={styles.warningCard}>
              <Text style={styles.warning}>
                Tu presión está fuera del rango recomendado por tu médico.
              </Text>
              <Text style={styles.warningDetail}>
                Si te sientes mal, avisa a tu familiar o consulta a tu médico.
              </Text>
            </View>
          ) : null}
        </View>
      ) : null}
    </AppCard>
  );
}

function StatusRow({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.statusRow}>
      <Text style={styles.statusLabel}>{label}</Text>
      <Text style={styles.statusValue}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  header: {
    alignItems: "flex-start",
    flexDirection: "row",
    justifyContent: "space-between",
    gap: 12,
  },
  headerText: {
    flex: 1,
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
    fontSize: 24,
    fontWeight: "900",
    marginTop: 4,
  },
  expandButton: {
    alignItems: "center",
    backgroundColor: colors.primarySoft,
    borderRadius: 20,
    flexDirection: "row",
    justifyContent: "space-between",
    minHeight: 54,
    paddingHorizontal: 16,
  },
  expandText: {
    color: colors.primary,
    fontSize: 17,
    fontWeight: "900",
  },
  details: {
    gap: 10,
  },
  statusRow: {
    alignItems: "center",
    backgroundColor: colors.cardWarm,
    borderRadius: 18,
    flexDirection: "row",
    justifyContent: "space-between",
    padding: 14,
  },
  statusLabel: {
    color: colors.text,
    fontSize: 17,
    fontWeight: "800",
  },
  statusValue: {
    color: colors.primary,
    flex: 1,
    fontSize: 17,
    fontWeight: "900",
    textAlign: "right",
  },
  muted: {
    color: colors.muted,
    fontSize: 15,
  },
  warningCard: {
    backgroundColor: colors.blue,
    borderRadius: 18,
    padding: 14,
  },
  warning: {
    color: colors.blueText,
    fontSize: 17,
    fontWeight: "800",
  },
  warningDetail: {
    color: colors.blueText,
    fontSize: 15,
    lineHeight: 21,
    marginTop: 8,
  },
});
