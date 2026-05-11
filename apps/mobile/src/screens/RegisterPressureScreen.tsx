import { useState } from "react";
import { StyleSheet, Text, TextInput, View } from "react-native";

import { AppButton } from "@/components/AppButton";
import { AppCard } from "@/components/AppCard";
import { Screen } from "@/components/Screen";
import { api } from "@/lib/api";
import { colors, radii } from "@/theme";

type RegisterPressureScreenProps = {
  onBack: () => void;
  onSaved: () => void | Promise<void>;
};

type SavedPressure = {
  systolic: number;
  diastolic: number;
  pulse?: number;
};

export function RegisterPressureScreen({
  onBack,
  onSaved,
}: RegisterPressureScreenProps) {
  const [systolic, setSystolic] = useState("");
  const [diastolic, setDiastolic] = useState("");
  const [pulse, setPulse] = useState("");
  const [notes, setNotes] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [savedPressure, setSavedPressure] = useState<SavedPressure | null>(null);

  async function handleSave() {
    const parsedSystolic = Number(systolic);
    const parsedDiastolic = Number(diastolic);
    const parsedPulse = pulse.trim() ? Number(pulse) : undefined;

    if (
      !Number.isInteger(parsedSystolic) ||
      !Number.isInteger(parsedDiastolic) ||
      parsedSystolic < 60 ||
      parsedSystolic > 250 ||
      parsedDiastolic < 40 ||
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
      await api.createBloodPressure({
        patientId: "patient_maria",
        systolic: parsedSystolic,
        diastolic: parsedDiastolic,
        ...(parsedPulse !== undefined ? { pulse: parsedPulse } : {}),
        notes: notes.trim() || undefined,
      });

      setSystolic("");
      setDiastolic("");
      setPulse("");
      setNotes("");
      await onSaved();
      setSavedPressure({
        systolic: parsedSystolic,
        diastolic: parsedDiastolic,
        pulse: parsedPulse,
      });
    } catch {
      setMessage("No se pudo registrar la presión. Intenta otra vez.");
    } finally {
      setIsSaving(false);
    }
  }

  if (savedPressure) {
    return (
      <Screen>
        <AppCard tone="green">
          <Text style={styles.successTitle}>Presión registrada correctamente</Text>
          <Text style={styles.successPressure}>
            {savedPressure.systolic}/{savedPressure.diastolic}
          </Text>
          {savedPressure.pulse ? (
            <Text style={styles.successPulse}>
              Pulso: {savedPressure.pulse} lpm
            </Text>
          ) : null}
          <AppButton label="Volver al inicio" onPress={onBack} />
          <AppButton
            label="Registrar otra"
            variant="secondary"
            onPress={() => setSavedPressure(null)}
          />
        </AppCard>
      </Screen>
    );
  }

  return (
    <Screen>
      <View style={styles.header}>
        <Text style={styles.eyebrow}>Presión</Text>
        <Text style={styles.title}>Registrar presión</Text>
        <Text style={styles.subtitle}>
          Ingresa tu presión alta, presión baja y pulso si lo tienes.
        </Text>
      </View>

      <AppCard>
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

        <View style={styles.field}>
          <Text style={styles.label}>Nota</Text>
          <TextInput
            value={notes}
            onChangeText={setNotes}
            placeholder="Ej. Después del desayuno"
            placeholderTextColor={colors.muted}
            multiline
            style={[styles.input, styles.textArea]}
          />
        </View>

        {message ? <Text style={styles.error}>{message}</Text> : null}

        <AppButton
          label="Guardar presión"
          loading={isSaving}
          loadingLabel="Guardando..."
          onPress={() => void handleSave()}
        />
        <AppButton label="Cancelar" variant="secondary" onPress={onBack} />
      </AppCard>
    </Screen>
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
  header: {
    gap: 8,
  },
  eyebrow: {
    alignSelf: "flex-start",
    backgroundColor: colors.primarySoft,
    borderRadius: 999,
    color: colors.primary,
    fontSize: 14,
    fontWeight: "900",
    letterSpacing: 1,
    paddingHorizontal: 14,
    paddingVertical: 8,
    textTransform: "uppercase",
  },
  title: {
    color: colors.text,
    fontSize: 36,
    fontWeight: "900",
    lineHeight: 42,
  },
  subtitle: {
    color: colors.muted,
    fontSize: 18,
    lineHeight: 26,
  },
  row: {
    flexDirection: "row",
    gap: 12,
  },
  field: {
    flex: 1,
    gap: 8,
  },
  label: {
    color: colors.text,
    fontSize: 17,
    fontWeight: "900",
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
    fontSize: 22,
    minHeight: 64,
    paddingHorizontal: 16,
  },
  textArea: {
    minHeight: 110,
    paddingTop: 16,
    textAlignVertical: "top",
  },
  error: {
    color: colors.redText,
    fontSize: 17,
    fontWeight: "900",
  },
  successTitle: {
    color: colors.text,
    fontSize: 30,
    fontWeight: "900",
    lineHeight: 36,
  },
  successPressure: {
    color: colors.primary,
    fontSize: 52,
    fontWeight: "900",
    letterSpacing: -1,
  },
  successPulse: {
    color: colors.text,
    fontSize: 22,
    fontWeight: "900",
  },
});
