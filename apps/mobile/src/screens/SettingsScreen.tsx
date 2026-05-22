import { useCallback, useEffect, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { useFocusEffect, useNavigation } from "@react-navigation/native";

import { AppButton } from "@/components/AppButton";
import { AppCard } from "@/components/AppCard";
import { Screen } from "@/components/Screen";
import {
  exportCuidaVozBackup,
  pickCuidaVozBackup,
  restoreBackup,
  shareBackup,
} from "@/lib/backup";
import {
  clearLocalRecords,
  getFamilyContact,
  getHealthSettings,
  getPatient,
  resetDemoDataLocal,
  updateFamilyContact,
  updateHealthSettings,
  updatePatient,
} from "@/lib/mobileData";
import {
  areMedicationRemindersEnabled,
  getReminderSettingsSummary,
  requestNotificationPermissions,
  scheduleMedicationReminders,
  scheduleTestNotification,
} from "@/lib/notifications";
import type { MainTabParamList } from "@/navigation/MainTabs";
import { colors, radii } from "@/theme";
import type { BottomTabNavigationProp } from "@react-navigation/bottom-tabs";

type ProfileForm = {
  fullName: string;
  age: string;
  notes: string;
};

type FamilyForm = {
  fullName: string;
  phone: string;
  relation: string;
};

type HealthForm = {
  systolicMinNormal: string;
  systolicMaxNormal: string;
  diastolicMinNormal: string;
  diastolicMaxNormal: string;
  pulseMinNormal: string;
  pulseMaxNormal: string;
  doctorRecommendation: string;
};

const emptyProfile: ProfileForm = {
  fullName: "",
  age: "",
  notes: "",
};

const emptyFamily: FamilyForm = {
  fullName: "",
  phone: "",
  relation: "",
};

const emptyHealth: HealthForm = {
  systolicMinNormal: "",
  systolicMaxNormal: "",
  diastolicMinNormal: "",
  diastolicMaxNormal: "",
  pulseMinNormal: "",
  pulseMaxNormal: "",
  doctorRecommendation: "",
};

function numberOrNull(value: string) {
  const normalized = value.trim();

  if (!normalized) {
    return null;
  }

  const parsed = Number(normalized);
  return Number.isFinite(parsed) ? parsed : NaN;
}

function toInputValue(value?: number | null) {
  return value === null || value === undefined ? "" : String(value);
}

export function SettingsScreen() {
  const navigation = useNavigation<BottomTabNavigationProp<MainTabParamList>>();
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [profile, setProfile] = useState<ProfileForm>(emptyProfile);
  const [family, setFamily] = useState<FamilyForm>(emptyFamily);
  const [health, setHealth] = useState<HealthForm>(emptyHealth);
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [isSavingFamily, setIsSavingFamily] = useState(false);
  const [isSavingHealth, setIsSavingHealth] = useState(false);
  const [isActivatingReminders, setIsActivatingReminders] = useState(false);
  const [isTestingNotification, setIsTestingNotification] = useState(false);
  const [isExportingBackup, setIsExportingBackup] = useState(false);
  const [isImportingBackup, setIsImportingBackup] = useState(false);
  const [isResettingDemo, setIsResettingDemo] = useState(false);
  const [isClearingData, setIsClearingData] = useState(false);
  const [remindersActivated, setRemindersActivated] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const reminderSettings = getReminderSettingsSummary();

  const loadSettings = useCallback(async () => {
    const [patient, familyContact, healthSettings, remindersEnabled] = await Promise.all([
      getPatient(),
      getFamilyContact(),
      getHealthSettings(),
      areMedicationRemindersEnabled(),
    ]);

    setProfile({
      fullName: patient?.fullName ?? "",
      age: patient?.age !== null && patient?.age !== undefined ? String(patient.age) : "",
      notes: patient?.notes ?? "",
    });

    setFamily({
      fullName: familyContact?.fullName ?? "Juan Rojas",
      phone: familyContact?.phone ?? "+51 999 999 999",
      relation: familyContact?.relation ?? "Familiar",
    });

    setHealth({
      systolicMinNormal: toInputValue(healthSettings?.systolicMinNormal),
      systolicMaxNormal: toInputValue(healthSettings?.systolicMaxNormal),
      diastolicMinNormal: toInputValue(healthSettings?.diastolicMinNormal),
      diastolicMaxNormal: toInputValue(healthSettings?.diastolicMaxNormal),
      pulseMinNormal: toInputValue(healthSettings?.pulseMinNormal),
      pulseMaxNormal: toInputValue(healthSettings?.pulseMaxNormal),
      doctorRecommendation: healthSettings?.doctorRecommendation ?? "",
    });

    setRemindersActivated(remindersEnabled);
    setLoadError(null);
  }, []);

  useEffect(() => {
    loadSettings()
      .catch(() => {
        setLoadError("No pudimos cargar los datos guardados en este celular.");
      })
      .finally(() => setIsLoading(false));
  }, [loadSettings]);

  useFocusEffect(
    useCallback(() => {
      void loadSettings().catch(() => {
        setLoadError("No pudimos actualizar los ajustes.");
      });
    }, [loadSettings])
  );

  async function refresh() {
    setIsRefreshing(true);
    try {
      await loadSettings();
    } catch {
      setLoadError("No pudimos actualizar los ajustes.");
    } finally {
      setIsRefreshing(false);
    }
  }

  async function handleSavePatient() {
    if (profile.fullName.trim().length < 2) {
      Alert.alert("CuidaVoz", "Escribe el nombre completo del paciente.");
      return;
    }

    const parsedAge = numberOrNull(profile.age);

    if (Number.isNaN(parsedAge) || (parsedAge !== null && parsedAge < 0)) {
      Alert.alert("CuidaVoz", "Revisa la edad antes de guardar.");
      return;
    }

    setIsSavingProfile(true);

    try {
      await updatePatient({
        fullName: profile.fullName.trim(),
        age: parsedAge,
        notes: profile.notes.trim() || null,
      });
      Alert.alert("CuidaVoz", "Datos del paciente guardados.");
    } catch {
      Alert.alert("CuidaVoz", "No pudimos guardar los datos del paciente.");
    } finally {
      setIsSavingProfile(false);
    }
  }

  async function handleSaveFamily() {
    if (family.fullName.trim().length < 2) {
      Alert.alert("CuidaVoz", "Escribe el nombre del familiar.");
      return;
    }

    if (!family.phone.trim()) {
      Alert.alert("CuidaVoz", "Escribe el teléfono del familiar.");
      return;
    }

    setIsSavingFamily(true);

    try {
      await updateFamilyContact({
        fullName: family.fullName.trim(),
        phone: family.phone.trim(),
        relation: family.relation.trim() || null,
      });
      Alert.alert("CuidaVoz", "Contacto familiar guardado.");
    } catch {
      Alert.alert("CuidaVoz", "No pudimos guardar el contacto familiar.");
    } finally {
      setIsSavingFamily(false);
    }
  }

  async function handleSaveHealthSettings() {
    const parsedValues = {
      systolicMinNormal: numberOrNull(health.systolicMinNormal),
      systolicMaxNormal: numberOrNull(health.systolicMaxNormal),
      diastolicMinNormal: numberOrNull(health.diastolicMinNormal),
      diastolicMaxNormal: numberOrNull(health.diastolicMaxNormal),
      pulseMinNormal: numberOrNull(health.pulseMinNormal),
      pulseMaxNormal: numberOrNull(health.pulseMaxNormal),
    };

    if (Object.values(parsedValues).some((value) => Number.isNaN(value))) {
      Alert.alert("CuidaVoz", "Revisa los rangos antes de guardar.");
      return;
    }

    setIsSavingHealth(true);

    try {
      await updateHealthSettings({
        ...parsedValues,
        doctorRecommendation: health.doctorRecommendation.trim() || null,
      });
      Alert.alert("CuidaVoz", "Rangos guardados correctamente.");
    } catch {
      Alert.alert("CuidaVoz", "No pudimos guardar los rangos. Intenta otra vez.");
    } finally {
      setIsSavingHealth(false);
    }
  }

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

      const result = await scheduleMedicationReminders();

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

  function confirmResetDemoData() {
    Alert.alert(
      "Restaurar datos de ejemplo",
      "Se volverán a cargar las pastillas, rangos y datos base de CuidaVoz en este celular.",
      [
        { text: "Cancelar", style: "cancel" },
        {
          text: "Restaurar",
          onPress: () => {
            setIsResettingDemo(true);
            resetDemoDataLocal()
              .then(async () => {
                await loadSettings();
                Alert.alert("CuidaVoz", "Datos de ejemplo restaurados.");
              })
              .catch(() => {
                Alert.alert("CuidaVoz", "No pudimos restaurar los datos.");
              })
              .finally(() => setIsResettingDemo(false));
          },
        },
      ]
    );
  }

  function confirmClearLocalData() {
    Alert.alert(
      "Borrar registros del dispositivo",
      "Se borrarán pastillas, historial, presión, rangos y recordatorios guardados en este celular. El perfil y el contacto familiar se conservarán.",
      [
        { text: "Cancelar", style: "cancel" },
        {
          text: "Borrar",
          style: "destructive",
          onPress: () => {
            setIsClearingData(true);
            clearLocalRecords()
              .then(async () => {
                await loadSettings();
                setRemindersActivated(false);
                Alert.alert("CuidaVoz", "Registros borrados del dispositivo.");
              })
              .catch(() => {
                Alert.alert("CuidaVoz", "No pudimos borrar los registros.");
              })
              .finally(() => setIsClearingData(false));
          },
        },
      ]
    );
  }

  async function handleExportBackup() {
    setIsExportingBackup(true);

    try {
      const backup = await exportCuidaVozBackup();
      try {
        await shareBackup(backup.uri);
      } catch (error) {
        if (error instanceof Error && error.message === "sharing_unavailable") {
          Alert.alert("CuidaVoz", "No se pudo abrir el panel para compartir.");
          return;
        }

        throw error;
      }

      Alert.alert("CuidaVoz", "Respaldo generado correctamente.");
    } catch {
      Alert.alert("CuidaVoz", "No se pudo generar el respaldo. Intenta otra vez.");
    } finally {
      setIsExportingBackup(false);
    }
  }

  function confirmImportBackup() {
    Alert.alert(
      "Importar respaldo",
      "Esto puede reemplazar los datos actuales de este celular. Te recomendamos exportar tus datos actuales antes de continuar.",
      [
        { text: "Cancelar", style: "cancel" },
        {
          text: "Elegir archivo",
          onPress: async () => {
            setIsImportingBackup(true);
            try {
              const backup = await pickCuidaVozBackup();

              if (backup === null) {
                return;
              }

              if (backup === "invalid") {
                Alert.alert(
                  "CuidaVoz",
                  "Este archivo no parece ser un respaldo válido de CuidaVoz."
                );
                return;
              }

              Alert.alert(
                "Restaurar datos",
                "Se restaurarán los datos del respaldo seleccionado.",
                [
                  { text: "Cancelar", style: "cancel" },
                  {
                    text: "Restaurar",
                    onPress: () => {
                      setIsImportingBackup(true);
                      restoreBackup(backup)
                        .then(async (result) => {
                          await loadSettings();
                          navigation.navigate("Inicio");
                          Alert.alert(
                            "CuidaVoz",
                            result.imageWarnings
                              ? "Los datos se importaron, pero algunas imágenes no pudieron restaurarse."
                              : "Datos importados correctamente."
                          );
                        })
                        .catch(() => {
                          Alert.alert(
                            "CuidaVoz",
                            "No se pudieron importar los datos. El respaldo no fue aplicado."
                          );
                        })
                        .finally(() => setIsImportingBackup(false));
                    },
                  },
                ]
              );
            } catch {
              Alert.alert(
                "CuidaVoz",
                "No se pudieron importar los datos. El respaldo no fue aplicado."
              );
            } finally {
              setIsImportingBackup(false);
            }
          },
        },
      ]
    );
  }

  if (isLoading) {
    return (
      <Screen scroll={false}>
        <View style={styles.loading}>
          <ActivityIndicator color={colors.primary} size="large" />
          <Text style={styles.loadingText}>Cargando ajustes...</Text>
        </View>
      </Screen>
    );
  }

  return (
    <Screen refreshing={isRefreshing} onRefresh={() => void refresh()}>
      <Text style={styles.eyebrow}>Ajustes</Text>
      <Text style={styles.title}>Centro de configuración</Text>
      <Text style={styles.subtitle}>
        Aquí puedes actualizar los datos importantes que usa CuidaVoz en este
        celular.
      </Text>

      {loadError ? <Text style={styles.errorBanner}>{loadError}</Text> : null}

      <AppCard>
        <Text style={styles.sectionTitle}>Datos del paciente</Text>
        <FormField
          label="Nombre completo"
          value={profile.fullName}
          onChangeText={(value) => setProfile((current) => ({ ...current, fullName: value }))}
          placeholder="María Rojas"
        />
        <FormField
          label="Edad"
          value={profile.age}
          onChangeText={(value) => setProfile((current) => ({ ...current, age: value }))}
          placeholder="72"
          keyboardType="number-pad"
        />
        <FormField
          label="Notas"
          value={profile.notes}
          onChangeText={(value) => setProfile((current) => ({ ...current, notes: value }))}
          placeholder="Paciente de prueba"
          multiline
        />
        <AppButton
          label="Guardar paciente"
          onPress={() => void handleSavePatient()}
          loading={isSavingProfile}
          loadingLabel="Guardando..."
        />
      </AppCard>

      <AppCard tone="blue">
        <Text style={styles.sectionTitle}>Contacto familiar</Text>
        <FormField
          label="Nombre del familiar"
          value={family.fullName}
          onChangeText={(value) => setFamily((current) => ({ ...current, fullName: value }))}
          placeholder="Juan Rojas"
        />
        <FormField
          label="Teléfono"
          value={family.phone}
          onChangeText={(value) => setFamily((current) => ({ ...current, phone: value }))}
          placeholder="+51 999 999 999"
          keyboardType="phone-pad"
        />
        <FormField
          label="Relación"
          value={family.relation}
          onChangeText={(value) => setFamily((current) => ({ ...current, relation: value }))}
          placeholder="Hijo"
        />
        <AppButton
          label="Guardar contacto"
          onPress={() => void handleSaveFamily()}
          loading={isSavingFamily}
          loadingLabel="Guardando..."
        />
      </AppCard>

      <AppCard tone="amber">
        <Text style={styles.sectionTitle}>Rangos indicados por el médico</Text>
        <Text style={styles.noteText}>
          Configura estos rangos según la indicación de un profesional de salud.
        </Text>
        <View style={styles.row}>
          <FormField
            label="Sistólica mínima normal"
            value={health.systolicMinNormal}
            onChangeText={(value) =>
              setHealth((current) => ({ ...current, systolicMinNormal: value }))
            }
            placeholder="100"
            keyboardType="number-pad"
            compact
          />
          <FormField
            label="Sistólica máxima normal"
            value={health.systolicMaxNormal}
            onChangeText={(value) =>
              setHealth((current) => ({ ...current, systolicMaxNormal: value }))
            }
            placeholder="130"
            keyboardType="number-pad"
            compact
          />
        </View>
        <View style={styles.row}>
          <FormField
            label="Diastólica mínima normal"
            value={health.diastolicMinNormal}
            onChangeText={(value) =>
              setHealth((current) => ({ ...current, diastolicMinNormal: value }))
            }
            placeholder="60"
            keyboardType="number-pad"
            compact
          />
          <FormField
            label="Diastólica máxima normal"
            value={health.diastolicMaxNormal}
            onChangeText={(value) =>
              setHealth((current) => ({ ...current, diastolicMaxNormal: value }))
            }
            placeholder="85"
            keyboardType="number-pad"
            compact
          />
        </View>
        <View style={styles.row}>
          <FormField
            label="Pulso mínimo normal"
            value={health.pulseMinNormal}
            onChangeText={(value) =>
              setHealth((current) => ({ ...current, pulseMinNormal: value }))
            }
            placeholder="60"
            keyboardType="number-pad"
            compact
          />
          <FormField
            label="Pulso máximo normal"
            value={health.pulseMaxNormal}
            onChangeText={(value) =>
              setHealth((current) => ({ ...current, pulseMaxNormal: value }))
            }
            placeholder="100"
            keyboardType="number-pad"
            compact
          />
        </View>
        <FormField
          label="Recomendación del médico"
          value={health.doctorRecommendation}
          onChangeText={(value) =>
            setHealth((current) => ({ ...current, doctorRecommendation: value }))
          }
          placeholder="Ej. Mantener la presión alrededor de 120/80"
          multiline
        />
        <AppButton
          label="Guardar rangos"
          onPress={() => void handleSaveHealthSettings()}
          loading={isSavingHealth}
          loadingLabel="Guardando..."
        />
      </AppCard>

      <AppCard tone="green">
        <Text style={styles.sectionTitle}>Recordatorios de pastillas</Text>
        <Text style={styles.bodyText}>
          CuidaVoz te avisará cuando sea hora de tomar tus pastillas.
        </Text>
        <Text style={styles.noteText}>
          CuidaVoz puede sonar, vibrar y recordarte tus pastillas. La voz se
          reproduce cuando la app está abierta o cuando entras desde la
          notificación.
        </Text>
        <Text style={styles.noteText}>
          Repetir cada {reminderSettings.repeatEveryMinutes} minutos
        </Text>
        <Text style={styles.noteText}>
          Número de avisos: {reminderSettings.repeatCount}
        </Text>
        <Text style={styles.noteText}>
          Voz al abrir desde recordatorio:{" "}
          {reminderSettings.speakOnOpen ? "Activado" : "Desactivado"}
        </Text>
        <Text style={styles.noteText}>
          Modo de voz: {reminderSettings.spokenModeLabel}
        </Text>
        <Text style={styles.noteText}>{reminderSettings.spokenDetail}</Text>
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
        <Text style={styles.sectionTitle}>Modo sin internet</Text>
        <Text style={styles.bodyText}>
          Tus datos se guardan en este celular. No necesitas conexión para usar
          CuidaVoz.
        </Text>
        <Text style={styles.noteText}>
          Si cambias de celular, los datos no se sincronizarán automáticamente.
        </Text>
      </AppCard>

      <AppCard tone="green">
        <Text style={styles.sectionTitle}>Respaldo de datos</Text>
        <Text style={styles.bodyText}>
          Guarda una copia de tus datos o pásalos a otro celular.
        </Text>
        <Text style={styles.noteText}>
          El respaldo puede contener información de salud. Compártelo solo con
          personas de confianza.
        </Text>
        <AppButton
          label="Exportar datos"
          onPress={() => void handleExportBackup()}
          loading={isExportingBackup}
          loadingLabel="Exportando..."
        />
        <AppButton
          label="Importar datos"
          variant="secondary"
          onPress={confirmImportBackup}
          loading={isImportingBackup}
          loadingLabel="Preparando..."
        />
      </AppCard>

      <AppCard tone="blue">
        <Text style={styles.sectionTitle}>Datos locales</Text>
        <Text style={styles.bodyText}>
          Puedes restaurar el escenario base o limpiar los registros guardados en
          este dispositivo.
        </Text>
        <AppButton
          label="Restaurar datos de ejemplo"
          variant="secondary"
          onPress={confirmResetDemoData}
          loading={isResettingDemo}
          loadingLabel="Restaurando..."
        />
        <AppButton
          label="Borrar registros del dispositivo"
          variant="danger"
          onPress={confirmClearLocalData}
          loading={isClearingData}
          loadingLabel="Borrando..."
        />
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

function FormField({
  label,
  value,
  onChangeText,
  placeholder,
  keyboardType,
  multiline,
  compact,
}: {
  label: string;
  value: string;
  onChangeText: (value: string) => void;
  placeholder: string;
  keyboardType?: "default" | "number-pad" | "phone-pad";
  multiline?: boolean;
  compact?: boolean;
}) {
  return (
    <View style={[styles.field, compact && styles.compactField]}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        value={value}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor={colors.muted}
        keyboardType={keyboardType}
        multiline={multiline}
        style={[
          styles.input,
          multiline && styles.textArea,
          compact && styles.compactInput,
        ]}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  loading: {
    alignItems: "center",
    flex: 1,
    gap: 16,
    justifyContent: "center",
    padding: 24,
  },
  loadingText: {
    color: colors.text,
    fontSize: 20,
    fontWeight: "800",
    textAlign: "center",
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
    fontSize: 34,
    fontWeight: "900",
  },
  subtitle: {
    color: colors.muted,
    fontSize: 18,
    lineHeight: 26,
  },
  errorBanner: {
    backgroundColor: colors.red,
    borderRadius: 18,
    color: colors.redText,
    fontSize: 16,
    fontWeight: "800",
    padding: 14,
  },
  sectionTitle: {
    color: colors.text,
    fontSize: 24,
    fontWeight: "900",
  },
  bodyText: {
    color: colors.text,
    fontSize: 17,
    lineHeight: 25,
  },
  noteText: {
    color: colors.muted,
    fontSize: 15,
    lineHeight: 22,
  },
  row: {
    flexDirection: "row",
    gap: 12,
  },
  field: {
    gap: 8,
  },
  compactField: {
    flex: 1,
  },
  label: {
    color: colors.text,
    fontSize: 17,
    fontWeight: "900",
  },
  input: {
    backgroundColor: "#fffdfa",
    borderColor: colors.border,
    borderRadius: radii.input,
    borderWidth: 1,
    color: colors.text,
    fontSize: 18,
    minHeight: 60,
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  compactInput: {
    minHeight: 58,
  },
  textArea: {
    minHeight: 104,
    textAlignVertical: "top",
  },
});
