import {
  Alert,
  Linking,
  Modal,
  Pressable,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useEffect, useState } from "react";

import { AppButton } from "@/components/AppButton";
import { getFamilyContact } from "@/lib/mobileData";
import { colors, radii, shadow } from "@/theme";

type HelpModalProps = {
  visible: boolean;
  onClose: () => void;
};

export function HelpModal({ visible, onClose }: HelpModalProps) {
  const [contact, setContact] = useState({
    fullName: "Juan Rojas",
    phone: "+51 999 999 999",
    relation: "Familiar",
  });

  useEffect(() => {
    if (!visible) {
      return;
    }

    getFamilyContact()
      .then((value) => {
        if (!value) {
          return;
        }

        setContact({
          fullName: value.fullName || "Juan Rojas",
          phone: value.phone || "+51 999 999 999",
          relation: value.relation || "Familiar",
        });
      })
      .catch(() => undefined);
  }, [visible]);

  async function callCaregiver() {
    try {
      const sanitizedPhone = contact.phone.replace(/\s+/g, "");
      await Linking.openURL(`tel:${sanitizedPhone}`);
    } catch {
      Alert.alert("CuidaVoz", "No se pudo iniciar la llamada.");
    }
  }

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <View style={styles.overlay}>
        <View style={styles.card}>
          <View style={styles.header}>
            <View style={styles.icon}>
              <Ionicons name="call" size={28} color={colors.primary} />
            </View>
            <Pressable accessibilityRole="button" onPress={onClose}>
              <Text style={styles.close}>Cerrar</Text>
            </Pressable>
          </View>

          <Text style={styles.title}>¿Necesitas ayuda?</Text>
          <Text style={styles.text}>Puedes llamar a tu familiar de confianza.</Text>

          <View style={styles.contactBox}>
            <Text style={styles.contactName}>{contact.fullName}</Text>
            <Text style={styles.relation}>{contact.relation}</Text>
            <Text style={styles.phone}>{contact.phone}</Text>
          </View>

          <AppButton label="Llamar ahora" onPress={() => void callCaregiver()} />
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    alignItems: "center",
    backgroundColor: "rgba(20, 33, 61, 0.35)",
    flex: 1,
    justifyContent: "center",
    padding: 20,
  },
  card: {
    backgroundColor: colors.background,
    borderRadius: radii.card,
    gap: 18,
    padding: 20,
    width: "100%",
    ...shadow,
  },
  header: {
    alignItems: "center",
    flexDirection: "row",
    justifyContent: "space-between",
  },
  icon: {
    alignItems: "center",
    backgroundColor: colors.primarySoft,
    borderRadius: 999,
    height: 58,
    justifyContent: "center",
    width: 58,
  },
  close: {
    color: colors.primary,
    fontSize: 17,
    fontWeight: "900",
  },
  title: {
    color: colors.text,
    fontSize: 32,
    fontWeight: "900",
    lineHeight: 38,
  },
  text: {
    color: colors.text,
    fontSize: 20,
    lineHeight: 28,
  },
  contactBox: {
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: 24,
    borderWidth: 1,
    gap: 6,
    padding: 18,
  },
  contactName: {
    color: colors.text,
    fontSize: 24,
    fontWeight: "900",
  },
  phone: {
    color: colors.primary,
    fontSize: 22,
    fontWeight: "900",
  },
  relation: {
    color: colors.muted,
    fontSize: 16,
    fontWeight: "700",
  },
});
