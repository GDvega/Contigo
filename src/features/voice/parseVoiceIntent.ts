import { parsePressure, type ParsedPressure } from "@/features/voice/parsePressure";

export type VoiceIntent =
  | {
      type: "blood_pressure";
      pressure: ParsedPressure;
      transcript: string;
    }
  | {
      type: "medication_taken";
      transcript: string;
    }
  | {
      type: "help";
      transcript: string;
    }
  | {
      type: "unknown";
      transcript: string;
    };

function normalizeIntent(text: string) {
  return text
    .toLowerCase()
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .replace(/[^\p{L}\p{N}\s]/gu, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function transcriptHintsMedicationTaken(normalized: string): boolean {
  if (
    /\bquiero\s+tomar\b/.test(normalized) ||
    /\btengo\s+que\s+tomar\b/.test(normalized) ||
    /\bdebo\s+tomar\b/.test(normalized) ||
    /\bvoy\s+a\s+tomar\b/.test(normalized)
  ) {
    return false;
  }

  const patterns = [
    /\bya\s+la\s+tome\b/,
    /\bya\s+lo\s+tome\b/,
    /\bya\s+los\s+tome\b/,
    /\bya\s+tome\s+mi\s+(pastilla|medicina|medicamento|pildora)\b/,
    /\bya\s+tome\s+la\s+(pastilla|medicina|medicamento)\b/,
    /\bya\s+tome\s+el\s+medicamento\b/,
    /\bya\s+tome\s+[a-z]{3,}\b/,
    /\btome\s+mi\s+(pastilla|medicina|medicamento|pildora)\b/,
    /\btome\s+la\s+(pastilla|medicina|medicamento)\b/,
    /\btome\s+el\s+medicamento\b/,
    /\btome\s+mi\s+medicina\b/,
    /\bsi\s*,\s*ya\s+tome\b/,
    /\bcorrecto\s*,\s*ya\s+tome\b/,
    /\blisto\s*,\s*ya\s+tome\b/,
  ];

  return patterns.some((pattern) => pattern.test(normalized));
}

function transcriptHintsHelp(normalized: string): boolean {
  const phrases = [
    "necesito ayuda",
    "necesito una ayuda",
    "quiero ayuda",
    "pido ayuda",
    "emergencia",
    "es una emergencia",
    "llamar a mi familiar",
    "llamar a mi familia",
    "llamar a mi hijo",
    "llamar a mi hija",
    "quiero llamar",
    "necesito llamar",
    "necesito que me llamen",
    "socorro",
    "auxilio",
  ];

  if (phrases.some((phrase) => normalized.includes(phrase))) {
    return true;
  }

  return normalized === "ayuda";
}

export function parseVoiceIntent(transcript: string): VoiceIntent {
  const trimmed = transcript.trim();
  const normalized = normalizeIntent(trimmed);

  const pressure = parsePressure(trimmed);
  if (pressure) {
    return { type: "blood_pressure", pressure, transcript: trimmed };
  }

  if (transcriptHintsMedicationTaken(normalized)) {
    return { type: "medication_taken", transcript: trimmed };
  }

  if (transcriptHintsHelp(normalized)) {
    return { type: "help", transcript: trimmed };
  }

  return { type: "unknown", transcript: trimmed };
}
