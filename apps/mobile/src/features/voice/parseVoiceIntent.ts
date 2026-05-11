import { parsePressure, type ParsedPressure } from "@/features/voice/parsePressure";

export type VoiceIntent =
  | {
      type: "blood_pressure";
      pressure: ParsedPressure;
    }
  | {
      type: "medication_taken";
    }
  | {
      type: "help";
    }
  | {
      type: "unknown";
    };

function normalize(text: string) {
  return text
    .toLowerCase()
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .replace(/[^\p{L}\p{N}/\s]/gu, " ")
    .replace(/\s+/g, " ")
    .trim();
}

export function parseVoiceIntent(text: string): VoiceIntent {
  const normalized = normalize(text);

  if (!normalized) {
    return { type: "unknown" };
  }

  if (
    [
      "necesito ayuda",
      "pedir ayuda",
      "llamar a mi familiar",
      "llama a mi familiar",
      "emergencia",
      "auxilio",
      "ayuda",
    ].some((phrase) => normalized.includes(phrase))
  ) {
    return { type: "help" };
  }

  const pressure = parsePressure(normalized);
  if (pressure) {
    return {
      type: "blood_pressure",
      pressure,
    };
  }

  if (
    [
      "ya tome mi pastilla",
      "ya tome la pastilla",
      "ya tome losartan",
      "ya la tome",
      "tome mi medicamento",
      "tome mi medicina",
      "tome la medicina",
      "si ya tome",
    ].some((phrase) => normalized.includes(phrase))
  ) {
    return { type: "medication_taken" };
  }

  return { type: "unknown" };
}
