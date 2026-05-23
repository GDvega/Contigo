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
  "no es correcto",
  "no guardar",
  "no registra",
  "no registres",
  "me equivoque",
  "esta mal",
  "no no",
  "negativo",
  "incorrecto",
  "repetir",
  "repite",
  "otra vez",
  "de nuevo",
  "cancelar",
  "cancela",
  "no",
];

const YES_PHRASES = [
  "correcto si",
  "si guardar",
  "si ya tome",
  "si tome",
  "si hijo",
  "esta bien",
  "muy bien",
  "de acuerdo",
  "eso es",
  "asi es",
  "es correcto",
  "ya la tome",
  "la tome",
  "tome la pastilla",
  "tome mi pastilla",
  "tome mi medicina",
  "tome mi medicamento",
  "ya tome mi medicamento",
  "ya tome losartan",
  "correcto",
  "confirmo",
  "confirmar",
  "confirmado",
  "conforme",
  "perfecto",
  "exacto",
  "afirmativo",
  "cierto",
  "verdad",
  "guardar",
  "guarda",
  "guardalo",
  "registra",
  "registrar",
  "registralo",
  "okay",
  "okey",
  "dale",
  "claro",
  "aja",
  "ujum",
  "umju",
  "listo hijo",
  "listo",
  "ya esta",
  "eso nomas",
  "eso",
  "ya tome",
  "bien",
  "ok",
  "si",
  "ya",
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
