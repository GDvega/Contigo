import { useEffect, useState } from "react";
import { ActivityIndicator, StyleSheet, Text, View } from "react-native";
import { StatusBar } from "expo-status-bar";
import { SafeAreaProvider } from "react-native-safe-area-context";

import { AppButton } from "@/components/AppButton";
import {
  initializeMobileData,
  recoverLocalData,
  resetMobileDataInitialization,
} from "@/lib/mobileData";
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
  const [isRecovering, setIsRecovering] = useState(false);
  const [isRetrying, setIsRetrying] = useState(false);

  async function prepareApp() {
    setHasError(false);
    setIsReady(false);

    try {
      await initializeMobileData();
      await rescheduleAllMedicationReminders().catch(() => undefined);
      setIsReady(true);
    } catch (error) {
      console.error("[App] Failed to prepare local data", error);
      setHasError(true);
    }
  }

  useEffect(() => {
    void prepareApp();
  }, []);

  async function retryPrepareApp() {
    setIsRetrying(true);
    resetMobileDataInitialization();
    try {
      await prepareApp();
    } finally {
      setIsRetrying(false);
    }
  }

  async function restoreLocalData() {
    setIsRecovering(true);
    try {
      await recoverLocalData();
      await rescheduleAllMedicationReminders().catch(() => undefined);
      setHasError(false);
      setIsReady(true);
    } catch (error) {
      console.error("[App] Failed to restore local data", error);
      setHasError(true);
    } finally {
      setIsRecovering(false);
    }
  }

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
          {!hasError ? (
            <ActivityIndicator color={colors.primary} size="large" />
          ) : null}
          <Text style={styles.text}>
            {hasError
              ? "No pudimos preparar los datos locales de CuidaVoz."
              : "Preparando CuidaVoz sin internet..."}
          </Text>
          {hasError ? (
            <View style={styles.actions}>
              <AppButton
                label="Reintentar"
                onPress={() => void retryPrepareApp()}
                loading={isRetrying}
                loadingLabel="Reintentando..."
              />
              <AppButton
                label="Restaurar datos locales"
                variant="secondary"
                onPress={() => void restoreLocalData()}
                loading={isRecovering}
                loadingLabel="Restaurando..."
              />
            </View>
          ) : null}
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
  actions: {
    gap: 12,
    width: "100%",
  },
});
