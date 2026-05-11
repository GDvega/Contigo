import { StyleSheet, Text, View } from "react-native";

import { colors, radii } from "@/theme";
import type { DailyRiskLevel, PressureStatus } from "@/types";

type StatusBadgeProps = {
  label: string;
  tone?: "green" | "amber" | "red" | "blue" | "neutral";
};

const pressureTone: Record<PressureStatus, StatusBadgeProps["tone"]> = {
  NORMAL: "green",
  ELEVATED: "amber",
  HIGH: "red",
  CRITICAL: "red",
};

const pressureLabels: Record<PressureStatus, string> = {
  NORMAL: "Normal",
  ELEVATED: "Elevada",
  HIGH: "Alta",
  CRITICAL: "Crítica",
};

const riskLabels: Record<DailyRiskLevel, string> = {
  low: "Bajo",
  medium: "Medio",
  high: "Alto",
};

export function StatusBadge({ label, tone = "neutral" }: StatusBadgeProps) {
  return (
    <View style={[styles.badge, tones[tone]]}>
      <Text style={[styles.label, labelTones[tone]]}>{label}</Text>
    </View>
  );
}

export function PressureStatusBadge({ status }: { status: PressureStatus }) {
  return <StatusBadge label={pressureLabels[status]} tone={pressureTone[status]} />;
}

export function RiskBadge({ risk }: { risk: DailyRiskLevel }) {
  const tone = risk === "low" ? "green" : risk === "medium" ? "amber" : "red";
  return <StatusBadge label={riskLabels[risk]} tone={tone} />;
}

const styles = StyleSheet.create({
  badge: {
    alignSelf: "flex-start",
    borderRadius: radii.chip,
    paddingHorizontal: 12,
    paddingVertical: 7,
  },
  label: {
    fontSize: 14,
    fontWeight: "800",
  },
});

const tones = StyleSheet.create({
  green: { backgroundColor: colors.green },
  amber: { backgroundColor: colors.amber },
  red: { backgroundColor: colors.red },
  blue: { backgroundColor: colors.blue },
  neutral: { backgroundColor: "#f3f4f6" },
});

const labelTones = StyleSheet.create({
  green: { color: colors.greenText },
  amber: { color: colors.amberText },
  red: { color: colors.redText },
  blue: { color: colors.blueText },
  neutral: { color: colors.muted },
});
