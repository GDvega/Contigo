import * as FileSystem from "expo-file-system/legacy";
import * as ImagePicker from "expo-image-picker";

const MEDICATION_IMAGES_DIR = `${FileSystem.documentDirectory}medications/`;

function getFileExtension(uri: string) {
  const match = uri.match(/\.([a-zA-Z0-9]+)(?:\?|$)/);
  return match?.[1]?.toLowerCase() ?? "jpg";
}

export async function ensureMedicationImagesDirectory() {
  const info = await FileSystem.getInfoAsync(MEDICATION_IMAGES_DIR);

  if (!info.exists) {
    await FileSystem.makeDirectoryAsync(MEDICATION_IMAGES_DIR, {
      intermediates: true,
    });
  }
}

export async function persistMedicationImage(sourceUri: string) {
  await ensureMedicationImagesDirectory();

  const extension = getFileExtension(sourceUri);
  const destination = `${MEDICATION_IMAGES_DIR}medication-${Date.now()}.${extension}`;

  await FileSystem.copyAsync({
    from: sourceUri,
    to: destination,
  });

  return destination;
}

export function getMedicationImagesDirectory() {
  return MEDICATION_IMAGES_DIR;
}

export async function deleteMedicationImage(uri?: string | null) {
  if (!uri || !uri.startsWith(MEDICATION_IMAGES_DIR)) {
    return;
  }

  const info = await FileSystem.getInfoAsync(uri);
  if (!info.exists) {
    return;
  }

  await FileSystem.deleteAsync(uri, { idempotent: true });
}

export async function pickImageFromGallery() {
  const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();

  if (!permission.granted) {
    throw new Error("gallery_permission_denied");
  }

  const result = await ImagePicker.launchImageLibraryAsync({
    mediaTypes: ["images"],
    quality: 0.8,
    allowsEditing: false,
    selectionLimit: 1,
  });

  if (result.canceled || !result.assets?.[0]?.uri) {
    return null;
  }

  return persistMedicationImage(result.assets[0].uri);
}

export async function pickImageFromCamera() {
  const permission = await ImagePicker.requestCameraPermissionsAsync();

  if (!permission.granted) {
    throw new Error("camera_permission_denied");
  }

  const result = await ImagePicker.launchCameraAsync({
    cameraType: ImagePicker.CameraType.back,
    mediaTypes: ["images"],
    quality: 0.8,
    allowsEditing: false,
  });

  if (result.canceled || !result.assets?.[0]?.uri) {
    return null;
  }

  return persistMedicationImage(result.assets[0].uri);
}
