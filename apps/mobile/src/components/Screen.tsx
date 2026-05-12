import type { ReactNode } from "react";
import type { RefreshControlProps } from "react-native";
import { RefreshControl, ScrollView, StyleSheet, View } from "react-native";
import { SafeAreaView, useSafeAreaInsets } from "react-native-safe-area-context";

import { colors, spacing } from "@/theme";

type ScreenProps = {
  children: ReactNode;
  refreshing?: boolean;
  onRefresh?: () => void;
  scroll?: boolean;
};

export function Screen({
  children,
  refreshing,
  onRefresh,
  scroll = true,
}: ScreenProps) {
  const insets = useSafeAreaInsets();
  const contentStyle = [
    styles.content,
    {
      paddingBottom: 120 + insets.bottom,
    },
  ];

  if (!scroll) {
    return (
      <SafeAreaView edges={["top", "left", "right"]} style={styles.safeArea}>
        <View style={contentStyle}>{children}</View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView edges={["top", "left", "right"]} style={styles.safeArea}>
      <ScrollView
        contentContainerStyle={contentStyle}
        keyboardShouldPersistTaps="handled"
        refreshControl={
          onRefresh ? (
            <RefreshControl
              refreshing={Boolean(refreshing)}
              onRefresh={onRefresh as RefreshControlProps["onRefresh"]}
            />
          ) : undefined
        }
        showsVerticalScrollIndicator={false}
      >
        {children}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: colors.background,
  },
  content: {
    gap: spacing.gap,
    padding: spacing.screen,
  },
});
