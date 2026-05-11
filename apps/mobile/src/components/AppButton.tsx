import type { ReactNode } from "react";
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  View,
} from "react-native";

import { colors, radii } from "@/theme";

type AppButtonProps = {
  label: string;
  onPress: () => void;
  variant?: "primary" | "secondary" | "soft" | "danger";
  disabled?: boolean;
  loading?: boolean;
  loadingLabel?: string;
  icon?: ReactNode;
};

export function AppButton({
  label,
  onPress,
  variant = "primary",
  disabled,
  loading,
  loadingLabel,
  icon,
}: AppButtonProps) {
  const isLightLabel = variant === "primary" || variant === "danger";

  return (
    <Pressable
      accessibilityRole="button"
      disabled={disabled || loading}
      onPress={onPress}
      style={({ pressed }) => [
        styles.button,
        variants[variant],
        (disabled || loading) && styles.disabled,
        pressed && styles.pressed,
      ]}
    >
      <View style={styles.content}>
        {loading ? (
          <ActivityIndicator color={isLightLabel ? "#fff" : colors.primary} />
        ) : (
          icon
        )}
        {loading && !loadingLabel ? null : (
          <Text
            style={[
              styles.label,
              isLightLabel ? styles.lightLabel : styles.darkLabel,
            ]}
          >
            {loading ? loadingLabel : label}
          </Text>
        )}
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    alignItems: "center",
    borderRadius: radii.button,
    justifyContent: "center",
    minHeight: 66,
    paddingHorizontal: 18,
  },
  content: {
    alignItems: "center",
    flexDirection: "row",
    gap: 10,
    justifyContent: "center",
  },
  label: {
    fontSize: 19,
    fontWeight: "800",
    flexShrink: 1,
    textAlign: "center",
  },
  lightLabel: {
    color: "#fff",
  },
  darkLabel: {
    color: colors.text,
  },
  disabled: {
    opacity: 0.55,
  },
  pressed: {
    transform: [{ scale: 0.99 }],
  },
});

const variants = StyleSheet.create({
  primary: {
    backgroundColor: colors.primary,
  },
  secondary: {
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderWidth: 1,
  },
  soft: {
    backgroundColor: colors.amber,
  },
  danger: {
    backgroundColor: "#b42318",
  },
});
