import { useEffect, useState } from "react";
import { ActivityIndicator, StyleSheet, Text, View } from "react-native";
import { StatusBar } from "expo-status-bar";
import { SafeAreaProvider } from "react-native-safe-area-context";

import { initializeMobileData } from "@/lib/mobileData";
import {
  attachNotificationListeners,
  configureNotificationBehavior,
  handleNotificationResponse,
  rescheduleAllMedicationReminders,
} from "@/lib/notifications";
import { MainTabs } from "@/navigation/MainTabs";
import { colors } from "@/theme";

export default function App() {
  const [isReady, setIsReady] = useState(false);
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    initializeMobileData()
      .then(async () => {
        await rescheduleAllMedicationReminders().catch(() => undefined);
        setIsReady(true);
      })
      .catch(() => setHasError(true));
  }, []);

  useEffect(() => {
    let isMounted = true;
    let cleanup: (() => void) | undefined;

    (async () => {
      await configureNotificationBehavior();
      cleanup = (await attachNotificationListeners()) ?? undefined;

      try {
        const Notifications = await import("expo-notifications");
        const lastResponse = await Notifications.getLastNotificationResponseAsync();

        if (isMounted && lastResponse) {
          await handleNotificationResponse(lastResponse);
        }
      } catch {
        // Ignore notification bootstrap failures and keep the app usable offline.
      }
    })().catch(() => undefined);

    return () => {
      isMounted = false;
      cleanup?.();
    };
  }, []);

  return (
    <SafeAreaProvider>
      <StatusBar style="dark" />
      {isReady ? (
        <MainTabs />
      ) : (
        <View style={styles.loading}>
          <ActivityIndicator color={colors.primary} size="large" />
          <Text style={styles.text}>
            {hasError
              ? "No pudimos preparar los datos locales de CuidaVoz."
              : "Preparando CuidaVoz sin internet..."}
          </Text>
        </View>
      )}
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  loading: {
    alignItems: "center",
    backgroundColor: colors.background,
    flex: 1,
    gap: 16,
    justifyContent: "center",
    padding: 24,
  },
  text: {
    color: colors.text,
    fontSize: 18,
    fontWeight: "800",
    textAlign: "center",
  },
});
