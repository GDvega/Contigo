import type { ReactNode } from "react";
import { StyleSheet, View } from "react-native";

import { colors, radii, shadow, spacing } from "@/theme";

type AppCardProps = {
  children: ReactNode;
  tone?: "default" | "amber" | "teal" | "blue" | "green" | "red";
};

export function AppCard({ children, tone = "default" }: AppCardProps) {
  return <View style={[styles.card, toneStyles[tone]]}>{children}</View>;
}

const styles = StyleSheet.create({
  card: {
    borderRadius: radii.card,
    gap: spacing.gap,
    padding: spacing.card,
    ...shadow,
  },
});

const toneStyles = StyleSheet.create({
  default: {
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderWidth: 1,
  },
  amber: {
    backgroundColor: colors.amber,
    borderColor: "#f5d07c",
    borderWidth: 1,
  },
  teal: {
    backgroundColor: colors.primary,
  },
  blue: {
    backgroundColor: colors.blue,
    borderColor: "#bce3f2",
    borderWidth: 1,
  },
  green: {
    backgroundColor: colors.green,
    borderColor: "#bde8cb",
    borderWidth: 1,
  },
  red: {
    backgroundColor: colors.red,
    borderColor: "#f7c5c5",
    borderWidth: 1,
  },
});
