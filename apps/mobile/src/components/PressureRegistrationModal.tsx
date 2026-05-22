import { useState } from "react";
import {
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";

import { AppButton } from "@/components/AppButton";
import { createBloodPressureReading } from "@/lib/mobileData";
import { colors, radii } from "@/theme";

type PressureRegistrationModalProps = {
  visible: boolean;
  onClose: () => void;
  onSaved: () => void;
};

export function PressureRegistrationModal({
  visible,
  onClose,
  onSaved,
}: PressureRegistrationModalProps) {
  const [systolic, setSystolic] = useState("");
  const [diastolic, setDiastolic] = useState("");
  const [pulse, setPulse] = useState("");
  const [notes, setNotes] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  async function handleSave() {
    const parsedSystolic = Number(systolic);
    const parsedDiastolic = Number(diastolic);
    const parsedPulse = pulse.trim() ? Number(pulse) : undefined;

    if (
      !Number.isInteger(parsedSystolic) ||
      !Number.isInteger(parsedDiastolic) ||
      parsedSystolic < 50 ||
      parsedSystolic > 250 ||
      parsedDiastolic < 30 ||
      parsedDiastolic > 160 ||
      (parsedPulse !== undefined &&
        (!Number.isInteger(parsedPulse) || parsedPulse < 30 || parsedPulse > 220))
    ) {
      setMessage("Revisa los valores antes de guardar.");
      return;
    }

    setIsSaving(true);
    setMessage(null);

    try {
      await createBloodPressureReading({
        patientId: "patient_maria",
        systolic: parsedSystolic,
        diastolic: parsedDiastolic,
        pulse: parsedPulse,
        notes: notes.trim() || undefined,
      });
      setSystolic("");
      setDiastolic("");
      setPulse("");
      setNotes("");
      onSaved();
      onClose();
    } catch {
      setMessage("No se pudo registrar la presión. Intenta otra vez.");
    } finally {
      setIsSaving(false);
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
            <Text style={styles.title}>Registrar presión</Text>
            <Pressable accessibilityRole="button" onPress={onClose}>
              <Text style={styles.close}>Cerrar</Text>
            </Pressable>
          </View>

          <View style={styles.row}>
            <Field
              label="Presión alta"
              value={systolic}
              onChange={setSystolic}
              placeholder="120"
            />
            <Field
              label="Presión baja"
              value={diastolic}
              onChange={setDiastolic}
              placeholder="80"
            />
          </View>
          <Field
            label="Pulso"
            value={pulse}
            onChange={setPulse}
            placeholder="72"
            optional
          />
          <Text style={styles.label}>Nota</Text>
          <TextInput
            value={notes}
            onChangeText={setNotes}
            placeholder="Ej. Después del desayuno"
            placeholderTextColor={colors.muted}
            multiline
            style={[styles.input, styles.textArea]}
          />

          {message ? <Text style={styles.error}>{message}</Text> : null}

          <AppButton
            label="Guardar presión"
            onPress={handleSave}
            loading={isSaving}
          />
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

function Field({
  label,
  value,
  placeholder,
  optional,
  onChange,
}: {
  label: string;
  value: string;
  placeholder: string;
  optional?: boolean;
  onChange: (value: string) => void;
}) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>
        {label} {optional ? <Text style={styles.optional}>Opcional</Text> : null}
      </Text>
      <TextInput
        value={value}
        onChangeText={onChange}
        keyboardType="number-pad"
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
    gap: 16,
    padding: 18,
  },
  header: {
    alignItems: "center",
    flexDirection: "row",
    justifyContent: "space-between",
  },
  title: {
    color: colors.text,
    fontSize: 28,
    fontWeight: "900",
  },
  close: {
    color: colors.primary,
    fontSize: 17,
    fontWeight: "800",
  },
  row: {
    flexDirection: "row",
    gap: 12,
  },
  field: {
    flex: 1,
    gap: 7,
  },
  label: {
    color: colors.text,
    fontSize: 16,
    fontWeight: "800",
  },
  optional: {
    color: colors.muted,
    fontSize: 14,
  },
  input: {
    backgroundColor: "#fffdfa",
    borderColor: colors.border,
    borderRadius: radii.input,
    borderWidth: 1,
    color: colors.text,
    fontSize: 20,
    minHeight: 58,
    paddingHorizontal: 14,
  },
  textArea: {
    minHeight: 90,
    paddingTop: 14,
    textAlignVertical: "top",
  },
  error: {
    color: colors.redText,
    fontSize: 16,
    fontWeight: "800",
  },
});
