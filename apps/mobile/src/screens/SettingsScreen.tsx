import { useState } from "react";
import { Alert, StyleSheet, Text } from "react-native";

import { AppButton } from "@/components/AppButton";
import { AppCard } from "@/components/AppCard";
import { Screen } from "@/components/Screen";
import { api } from "@/lib/api";
import {
  requestNotificationPermissions,
  scheduleMedicationNotifications,
  scheduleTestNotification,
} from "@/lib/notifications";
import { colors } from "@/theme";

export function SettingsScreen() {
  const [isActivatingReminders, setIsActivatingReminders] = useState(false);
  const [isTestingNotification, setIsTestingNotification] = useState(false);
  const [remindersActivated, setRemindersActivated] = useState(false);

  async function activateReminders() {
    setIsActivatingReminders(true);

    try {
      const permission = await requestNotificationPermissions();

      if (!permission.granted) {
        Alert.alert(
          "CuidaVoz",
          permission.reason === "expo_go_not_supported"
            ? "Para probar recordatorios reales se necesita una versión instalada de la app."
            : "No se otorgó permiso para enviar recordatorios."
        );
        return;
      }

      const medications = await api.getMedications();
      const result = await scheduleMedicationNotifications(medications);

      if (!result.scheduled) {
        Alert.alert(
          "CuidaVoz",
          "Para probar recordatorios reales se necesita una versión instalada de la app."
        );
        return;
      }

      setRemindersActivated(true);
      Alert.alert("CuidaVoz", "Recordatorios activados.");
    } catch {
      Alert.alert("CuidaVoz", "No se pudieron activar los recordatorios.");
    } finally {
      setIsActivatingReminders(false);
    }
  }

  async function testNotification() {
    setIsTestingNotification(true);

    try {
      const permission = await requestNotificationPermissions();

      if (!permission.granted) {
        Alert.alert(
          "CuidaVoz",
          permission.reason === "expo_go_not_supported"
            ? "Para probar recordatorios reales se necesita una versión instalada de la app."
            : "No se otorgó permiso para enviar recordatorios."
        );
        return;
      }

      const result = await scheduleTestNotification();

      if (!result.scheduled) {
        Alert.alert(
          "CuidaVoz",
          "Para probar recordatorios reales se necesita una versión instalada de la app."
        );
        return;
      }

      Alert.alert("CuidaVoz", "Notificación de prueba programada.");
    } catch {
      Alert.alert("CuidaVoz", "No se pudo programar la notificación de prueba.");
    } finally {
      setIsTestingNotification(false);
    }
  }

  return (
    <Screen>
      <Text style={styles.eyebrow}>Ajustes</Text>
      <Text style={styles.title}>Ajustes</Text>
      <AppCard tone="green">
        <Text style={styles.sectionTitle}>Recordatorios</Text>
        <Text style={styles.bodyText}>
          CuidaVoz puede avisarte cuando sea hora de tomar tus pastillas.
        </Text>
        <AppButton
          label={
            remindersActivated ? "Recordatorios activados" : "Activar recordatorios"
          }
          loading={isActivatingReminders}
          loadingLabel="Activando..."
          variant={remindersActivated ? "secondary" : "primary"}
          onPress={() => void activateReminders()}
        />
        {__DEV__ ? (
          <AppButton
            label="Probar notificación"
            loading={isTestingNotification}
            loadingLabel="Programando..."
            variant="secondary"
            onPress={() => void testNotification()}
          />
        ) : null}
      </AppCard>
      <AppCard>
        <Text style={styles.sectionTitle}>Datos sincronizados</Text>
        <Text style={styles.bodyText}>
          CuidaVoz usa la información configurada por la familia.
        </Text>
      </AppCard>
      <AppCard tone="blue">
        <Text style={styles.sectionTitle}>Funciones próximas</Text>
        <Text style={styles.bodyText}>
          Voz, notificaciones avanzadas y modo sin internet estarán disponibles
          en próximas versiones.
        </Text>
      </AppCard>
      <AppCard tone="amber">
        <Text style={styles.sectionTitle}>Seguridad</Text>
        <Text style={styles.bodyText}>
          CuidaVoz es una herramienta de apoyo. No reemplaza la evaluación de un
          profesional de salud.
        </Text>
      </AppCard>
    </Screen>
  );
}

const styles = StyleSheet.create({
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
  },
  sectionTitle: {
    color: colors.text,
    fontSize: 23,
    fontWeight: "900",
  },
  bodyText: {
    color: colors.text,
    fontSize: 17,
    lineHeight: 25,
  },
});
