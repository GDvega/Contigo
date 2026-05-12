import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Image,
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";

import { AppButton } from "@/components/AppButton";
import { createMedication, updateMedication } from "@/lib/mobileData";
import {
  pickImageFromCamera,
  pickImageFromGallery,
} from "@/lib/localMedicationImages";
import { colors, radii } from "@/theme";
import type { Medication } from "@/types";

type MedicationEditorModalProps = {
  visible: boolean;
  medication?: Medication | null;
  onClose: () => void;
  onSaved: () => void | Promise<void>;
};

function to12HourParts(time: string) {
  const [hourText = "7", minute = "00"] = time.split(":");
  const hour24 = Number(hourText);
  const period = hour24 >= 12 ? "PM" : "AM";
  const hour12 = hour24 % 12 === 0 ? 12 : hour24 % 12;

  return {
    hour: String(hour12),
    minute,
    period: period as "AM" | "PM",
  };
}

function to24Hour(hour: string, minute: string, period: "AM" | "PM") {
  const parsedHour = Number(hour);
  const parsedMinute = Number(minute);

  if (
    !Number.isInteger(parsedHour) ||
    parsedHour < 1 ||
    parsedHour > 12 ||
    !Number.isInteger(parsedMinute) ||
    parsedMinute < 0 ||
    parsedMinute > 59
  ) {
    return null;
  }

  let hour24 = parsedHour % 12;
  if (period === "PM") {
    hour24 += 12;
  }

  return `${String(hour24).padStart(2, "0")}:${String(parsedMinute).padStart(2, "0")}`;
}

export function MedicationEditorModal({
  visible,
  medication,
  onClose,
  onSaved,
}: MedicationEditorModalProps) {
  const initialTime = medication?.schedules.find((schedule) => schedule.isActive)?.time ?? "07:00";
  const timeParts = useMemo(() => to12HourParts(initialTime), [initialTime]);
  const [name, setName] = useState("");
  const [dose, setDose] = useState("");
  const [hour, setHour] = useState("7");
  const [minute, setMinute] = useState("00");
  const [period, setPeriod] = useState<"AM" | "PM">("AM");
  const [color, setColor] = useState("");
  const [shape, setShape] = useState("");
  const [instructions, setInstructions] = useState("");
  const [imageUri, setImageUri] = useState<string | null>(null);
  const [imageLoadFailed, setImageLoadFailed] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    if (!visible) {
      return;
    }

    setName(medication?.name ?? "");
    setDose(medication?.dose ?? "");
    setHour(timeParts.hour);
    setMinute(timeParts.minute);
    setPeriod(timeParts.period);
    setColor(medication?.color ?? "");
    setShape(medication?.shape ?? "");
    setInstructions(medication?.instructions ?? "");
    setImageUri(medication?.imageUri ?? null);
    setImageLoadFailed(false);
    setMessage(null);
    setIsSaving(false);
  }, [medication, timeParts.hour, timeParts.minute, timeParts.period, visible]);

  async function handleSave() {
    const formattedTime = to24Hour(hour.trim(), minute.trim(), period);

    if (!name.trim() || !dose.trim() || !formattedTime) {
      setMessage("Completa nombre, dosis y hora antes de guardar.");
      return;
    }

    setIsSaving(true);
    setMessage(null);

    try {
      if (medication) {
        await updateMedication(medication.id, {
          name: name.trim(),
          dose: dose.trim(),
          time: formattedTime,
          color: color.trim() || null,
          shape: shape.trim() || null,
          instructions: instructions.trim() || null,
          imageUri,
        });
      } else {
        await createMedication({
          name: name.trim(),
          dose: dose.trim(),
          time: formattedTime,
          color: color.trim() || null,
          shape: shape.trim() || null,
          instructions: instructions.trim() || null,
          imageUri,
        });
      }

      await onSaved();
      onClose();
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : "No pudimos guardar los datos en este dispositivo."
      );
    } finally {
      setIsSaving(false);
    }
  }

  async function handlePickFromCamera() {
    try {
      const uri = await pickImageFromCamera();
      if (!uri) {
        return;
      }

      setImageUri(uri);
      setImageLoadFailed(false);
    } catch (error) {
      if (error instanceof Error && error.message === "camera_permission_denied") {
        Alert.alert("CuidaVoz", "No se otorgó permiso para usar la cámara.");
        return;
      }

      Alert.alert(
        "CuidaVoz",
        "No pudimos guardar la imagen. Puedes guardar la pastilla sin foto."
      );
    }
  }

  async function handlePickFromGallery() {
    try {
      const uri = await pickImageFromGallery();
      if (!uri) {
        return;
      }

      setImageUri(uri);
      setImageLoadFailed(false);
    } catch (error) {
      if (error instanceof Error && error.message === "gallery_permission_denied") {
        Alert.alert("CuidaVoz", "No se otorgó permiso para acceder a tus fotos.");
        return;
      }

      Alert.alert(
        "CuidaVoz",
        "No pudimos guardar la imagen. Puedes guardar la pastilla sin foto."
      );
    }
  }

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <KeyboardAvoidingView
        behavior={Platform.OS === "ios" ? "padding" : undefined}
        style={styles.overlay}
      >
        <View style={styles.sheet}>
          <View style={styles.header}>
            <Text style={styles.title}>
              {medication ? "Editar pastilla" : "Agregar pastilla"}
            </Text>
            <Pressable accessibilityRole="button" onPress={onClose}>
              <Text style={styles.close}>Cerrar</Text>
            </Pressable>
          </View>

          <ScrollView contentContainerStyle={styles.content}>
            <Field
              label="Nombre"
              value={name}
              onChange={setName}
              placeholder="Ej. Losartán"
            />
            <Field
              label="Dosis"
              value={dose}
              onChange={setDose}
              placeholder="Ej. 1 tableta"
            />

            <View style={styles.timeCard}>
              <Text style={styles.label}>Hora</Text>
              <View style={styles.timeRow}>
                <View style={styles.timeColumn}>
                  <TextInput
                    value={hour}
                    onChangeText={setHour}
                    keyboardType="number-pad"
                    placeholder="7"
                    placeholderTextColor={colors.muted}
                    style={styles.input}
                  />
                  <Text style={styles.helper}>Hora</Text>
                </View>
                <Text style={styles.timeSeparator}>:</Text>
                <View style={styles.timeColumn}>
                  <TextInput
                    value={minute}
                    onChangeText={setMinute}
                    keyboardType="number-pad"
                    placeholder="00"
                    placeholderTextColor={colors.muted}
                    style={styles.input}
                  />
                  <Text style={styles.helper}>Minutos</Text>
                </View>
              </View>
              <View style={styles.periodRow}>
                {(["AM", "PM"] as const).map((value) => (
                  <Pressable
                    key={value}
                    accessibilityRole="button"
                    onPress={() => setPeriod(value)}
                    style={[
                      styles.periodButton,
                      period === value && styles.periodButtonActive,
                    ]}
                  >
                    <Text
                      style={[
                        styles.periodText,
                        period === value && styles.periodTextActive,
                      ]}
                    >
                      {value}
                    </Text>
                  </Pressable>
                ))}
              </View>
            </View>

            <Field
              label="Color"
              value={color}
              onChange={setColor}
              placeholder="Ej. Blanca"
            />
            <Field
              label="Forma"
              value={shape}
              onChange={setShape}
              placeholder="Ej. Ovalada"
            />

            <View style={styles.field}>
              <Text style={styles.label}>Instrucciones</Text>
              <TextInput
                value={instructions}
                onChangeText={setInstructions}
                placeholder="Ej. Tomar después del desayuno"
                placeholderTextColor={colors.muted}
                multiline
                style={[styles.input, styles.textArea]}
              />
            </View>

            <View style={styles.imageSection}>
              <Text style={styles.label}>Imagen del medicamento</Text>
              {imageUri && !imageLoadFailed ? (
                <Image
                  source={{ uri: imageUri }}
                  style={styles.preview}
                  onError={() => setImageLoadFailed(true)}
                />
              ) : (
                <View style={styles.previewFallback}>
                  <Ionicons name="medkit" size={42} color={colors.primary} />
                  <Text style={styles.previewText}>Sin imagen</Text>
                </View>
              )}

              <View style={styles.imageButtons}>
                <AppButton
                  label="Tomar foto"
                  variant="secondary"
                  onPress={() => void handlePickFromCamera()}
                />
                <AppButton
                  label="Elegir de galería"
                  variant="secondary"
                  onPress={() => void handlePickFromGallery()}
                />
                <AppButton
                  label="Quitar imagen"
                  variant="soft"
                  onPress={() => {
                    setImageUri(null);
                    setImageLoadFailed(false);
                  }}
                />
              </View>
            </View>

            {message ? <Text style={styles.error}>{message}</Text> : null}

            <AppButton
              label="Guardar pastilla"
              loading={isSaving}
              loadingLabel="Guardando..."
              onPress={() => void handleSave()}
            />
          </ScrollView>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

function Field({
  label,
  value,
  placeholder,
  onChange,
}: {
  label: string;
  value: string;
  placeholder: string;
  onChange: (value: string) => void;
}) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        value={value}
        onChangeText={onChange}
        placeholder={placeholder}
        placeholderTextColor={colors.muted}
        style={styles.input}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  overlay: {
    backgroundColor: "rgba(20, 33, 61, 0.28)",
    flex: 1,
    justifyContent: "flex-end",
  },
  sheet: {
    backgroundColor: colors.background,
    borderTopLeftRadius: 32,
    borderTopRightRadius: 32,
    maxHeight: "94%",
    paddingBottom: 18,
  },
  header: {
    alignItems: "center",
    flexDirection: "row",
    justifyContent: "space-between",
    paddingHorizontal: 18,
    paddingTop: 18,
  },
  title: {
    color: colors.text,
    fontSize: 28,
    fontWeight: "900",
  },
  close: {
    color: colors.primary,
    fontSize: 18,
    fontWeight: "900",
  },
  content: {
    gap: 16,
    padding: 18,
    paddingBottom: 30,
  },
  field: {
    gap: 8,
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
    fontSize: 20,
    minHeight: 62,
    paddingHorizontal: 16,
  },
  textArea: {
    minHeight: 110,
    paddingTop: 16,
    textAlignVertical: "top",
  },
  timeCard: {
    gap: 10,
  },
  timeRow: {
    alignItems: "center",
    flexDirection: "row",
    gap: 12,
  },
  timeColumn: {
    flex: 1,
    gap: 6,
  },
  helper: {
    color: colors.muted,
    fontSize: 15,
    fontWeight: "700",
    textAlign: "center",
  },
  timeSeparator: {
    color: colors.text,
    fontSize: 34,
    fontWeight: "900",
    marginTop: -12,
  },
  periodRow: {
    flexDirection: "row",
    gap: 12,
  },
  periodButton: {
    alignItems: "center",
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: 18,
    borderWidth: 1,
    flex: 1,
    minHeight: 58,
    justifyContent: "center",
  },
  periodButtonActive: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  periodText: {
    color: colors.text,
    fontSize: 18,
    fontWeight: "900",
  },
  periodTextActive: {
    color: "#fff",
  },
  imageSection: {
    gap: 12,
  },
  preview: {
    alignSelf: "center",
    borderRadius: 28,
    height: 180,
    width: 180,
  },
  previewFallback: {
    alignItems: "center",
    alignSelf: "center",
    backgroundColor: colors.primarySoft,
    borderRadius: 28,
    gap: 8,
    height: 180,
    justifyContent: "center",
    width: 180,
  },
  previewText: {
    color: colors.primary,
    fontSize: 18,
    fontWeight: "800",
  },
  imageButtons: {
    gap: 10,
  },
  error: {
    color: colors.redText,
    fontSize: 17,
    fontWeight: "900",
  },
});
