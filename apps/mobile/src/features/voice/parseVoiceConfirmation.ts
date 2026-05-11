function normalizeTranscript(text: string) {
  return text
    .toLowerCase()
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .replace(/[^\p{L}\p{N}\s]/gu, " ")
    .replace(/\s+/g, " ")
    .trim();
}

const NO_PHRASES = [
  "no guardar",
  "me equivoque",
  "otra vez",
  "esta mal",
  "incorrecto",
  "cancelar",
  "repetir",
  "no",
];

const YES_PHRASES = [
  "esta bien",
  "confirmado",
  "correcto",
  "guardar",
  "perfecto",
  "exacto",
  "listo",
  "dale",
  "ok",
  "si",
];

function includesPhrase(transcript: string, phrase: string) {
  return ` ${transcript} `.includes(` ${phrase} `);
}

export function parseVoiceConfirmation(text: string): "yes" | "no" | null {
  const normalized = normalizeTranscript(text);

  if (!normalized) {
    return null;
  }

  if (NO_PHRASES.some((phrase) => includesPhrase(normalized, phrase))) {
    return "no";
  }

  if (YES_PHRASES.some((phrase) => includesPhrase(normalized, phrase))) {
    return "yes";
  }

  return null;
}
