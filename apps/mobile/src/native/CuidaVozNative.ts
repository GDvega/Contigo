import { NativeModules, Platform } from "react-native";

const { CuidaVozNative } = NativeModules;

/**
 * Interfaz para interactuar con el código nativo de Android (AlarmManager, TTS, etc.)
 */
export const scheduleNativeAlarm = async (
  id: string,
  timeMillis: number,
  message: string
): Promise<boolean> => {
  if (Platform.OS !== "android") {
    return false;
  }

  try {
    return await CuidaVozNative.scheduleAlarm(id, timeMillis, message);
  } catch (error) {
    console.error("[CuidaVozNative] Error scheduling alarm:", error);
    return false;
  }
};

export const cancelNativeAlarm = (id: string) => {
  if (Platform.OS !== "android") {
    return;
  }
  CuidaVozNative.cancelAlarm(id);
};

export default CuidaVozNative;
