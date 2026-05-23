export type ParsedPressure = {
  systolic: number;
  diastolic: number;
  pulse?: number;
};

const numberWords: Record<string, number> = {
  cero: 0,
  un: 1,
  uno: 1,
  una: 1,
  dos: 2,
  tres: 3,
  cuatro: 4,
  cinco: 5,
  seis: 6,
  siete: 7,
  ocho: 8,
  nueve: 9,
  diez: 10,
  once: 11,
  doce: 12,
  trece: 13,
  catorce: 14,
  quince: 15,
  dieciseis: 16,
  dieciséis: 16,
  diecisiete: 17,
  dieciocho: 18,
  diecinueve: 19,
  veinte: 20,
  veintiuno: 21,
  veintiun: 21,
  veintiuna: 21,
  veintidos: 22,
  veintitres: 23,
  veinticuatro: 24,
  veinticinco: 25,
  veintiseis: 26,
  veintisiete: 27,
  veintiocho: 28,
  veintinueve: 29,
  treinta: 30,
  cuarenta: 40,
  cincuenta: 50,
  sesenta: 60,
  setenta: 70,
  ochenta: 80,
  noventa: 90,
  cien: 100,
  ciento: 100,
  doscientos: 200,
};

/** Multi-word phrases must be listed before single-word alternates in some patterns. */
const pulseKeywordPattern =
  "(?:frecuencia\\s+cardiaca|pulsaciones|pulsacion|pulso|pulsos|puso|frecuencia|latidos|ritmo)";

/**
 * Optional Spanish fillers between pulse keyword and the value (digit or word number).
 * Order: longer phrases first (e.g. "es de" before "es" or "de" alone).
 */
const pulseValueLeadIn =
  "(?:es\\s+aproximadamente|esta\\s+en|es\\s+de|son\\s+de|aproximadamente|son|es|de)?";

function normalize(text: string) {
  return text
    .toLowerCase()
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .replace(/[^\p{L}\p{N}/\s]/gu, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function parseNumberWords(text: string) {
  const compact = normalize(text)
    .split(/\s+/)
    .filter((word) => word !== "y")
    .join(" ");
  const direct = numberWords[compact];

  if (direct !== undefined) {
    return direct;
  }

  const words = compact.split(/\s+/);
  let total = 0;
  let foundNumber = false;

  for (const word of words) {
    const value = numberWords[word];

    if (value === undefined) {
      if (foundNumber) {
        break;
      }

      continue;
    }

    foundNumber = true;
    total += value;
  }

  return foundNumber ? total : undefined;
}

function isValidPressure(systolic: number, diastolic: number) {
  return systolic >= 60 && systolic <= 250 && diastolic >= 40 && diastolic <= 160;
}

export function isValidPulse(pulse: number) {
  return pulse >= 30 && pulse <= 220;
}

function parsePulse(normalized: string) {
  const numericAfterPulse = normalized.match(
    new RegExp(
      `\\b${pulseKeywordPattern}\\s+${pulseValueLeadIn}\\s*(\\d{2,3})\\b`
    )
  );

  if (numericAfterPulse) {
    const pulse = Number(numericAfterPulse[1]);
    return isValidPulse(pulse) ? pulse : undefined;
  }

  const wordAfterPulse = normalized.match(
    new RegExp(
      `\\b${pulseKeywordPattern}\\s+${pulseValueLeadIn}\\s+([a-z]+(?:\\s+(?:y\\s+)?[a-z]+){0,4})\\b`
    )
  );

  if (wordAfterPulse) {
    const pulse = parseNumberWords(wordAfterPulse[1]);

    if (pulse !== undefined && isValidPulse(pulse)) {
      return pulse;
    }
  }

  const numericBeforePulse = normalized.match(
    new RegExp(`\\b(\\d{2,3})\\s+(?:de\\s+)?${pulseKeywordPattern}\\b`)
  );

  if (numericBeforePulse) {
    const pulse = Number(numericBeforePulse[1]);
    return isValidPulse(pulse) ? pulse : undefined;
  }

  return undefined;
}

function withOptionalPulse(
  systolic: number,
  diastolic: number,
  pulse?: number
): ParsedPressure | null {
  if (!isValidPressure(systolic, diastolic)) {
    return null;
  }

  return {
    systolic,
    diastolic,
    ...(pulse !== undefined && isValidPulse(pulse) ? { pulse } : {}),
  };
}

export function parsePressure(text: string): ParsedPressure | null {
  const normalized = normalize(text);
  const explicitPulse = parsePulse(normalized);

  const numericMatch = normalized.match(
    /\b(\d{2,3})\s*(?:\/|sobre|con)\s*(\d{2,3})\b/
  );

  const numbers = normalized.match(/\b\d{2,3}\b/g)?.map(Number) ?? [];

  if (process.env.NODE_ENV === "development") {
    console.log("[parsePressure] original", text);
    console.log("[parsePressure] normalized", normalized);
    console.log("[parsePressure] explicitPulse", explicitPulse);
    console.log("[parsePressure] numericSeparatorMatch", numericMatch);
    console.log("[parsePressure] allNumbers", numbers);
  }

  if (
    normalized.includes("por ejemplo") &&
    !normalized.includes("mi presion") &&
    !normalized.includes("tengo")
  ) {
    if (process.env.NODE_ENV === "development") {
      console.log("[parsePressure] rejected (instruction-only phrase)", null);
    }
    return null;
  }

  if (numericMatch) {
    const systolic = Number(numericMatch[1]);
    const diastolic = Number(numericMatch[2]);
    const thirdAfterPair =
      numbers.length >= 3 &&
      numbers[0] === systolic &&
      numbers[1] === diastolic
        ? numbers[2]
        : undefined;
    const pulseCandidate = explicitPulse ?? thirdAfterPair;
    const parsed = withOptionalPulse(systolic, diastolic, pulseCandidate);

    if (process.env.NODE_ENV === "development") {
      console.log("[parsePressure] branch", "separator");
      console.log("[parsePressure] pulseCandidate", pulseCandidate);
      console.log("[parsePressure] result", parsed);
    }

    if (parsed) {
      return parsed;
    }

    return null;
  }

  if (numbers.length >= 2) {
    const [systolic, diastolic, fallbackPulse] = numbers;
    const parsed = withOptionalPulse(
      systolic,
      diastolic,
      explicitPulse ?? fallbackPulse
    );

    if (process.env.NODE_ENV === "development") {
      console.log("[parsePressure] branch", "numericFallback");
      console.log("[parsePressure] result", parsed);
    }

    if (parsed) {
      return parsed;
    }
  }

  const wordMatch = normalized.match(
    new RegExp(
      `(?:presion es|presion|tengo|es)?\\s*([a-z\\s]+?)\\s+sobre\\s+([a-z\\s]+?)(?:\\s+(?:y\\s+mi\\s+)?${pulseKeywordPattern}\\b|$)`
    )
  );

  if (!wordMatch) {
    if (process.env.NODE_ENV === "development") {
      console.log("[parsePressure] branch", "wordPressure-noMatch");
      console.log("[parsePressure] result", null);
    }
    return null;
  }

  const systolic = parseNumberWords(wordMatch[1]);
  const diastolic = parseNumberWords(wordMatch[2]);

  if (systolic === undefined || diastolic === undefined) {
    if (process.env.NODE_ENV === "development") {
      console.log("[parsePressure] branch", "wordPressure-invalidWords");
      console.log("[parsePressure] result", null);
    }
    return null;
  }

  const parsed = withOptionalPulse(systolic, diastolic, explicitPulse);

  if (process.env.NODE_ENV === "development") {
    console.log("[parsePressure] branch", "wordPressure");
    console.log("[parsePressure] result", parsed);
  }

  return parsed;
}

/**
 * True when the user may still be speaking (e.g. about to add pulse after "y").
 */
export function looksLikeIncompleteVoiceCommandTranscript(transcriptText: string) {
  const lastWord = transcriptText
    .toLowerCase()
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .replace(/[^\p{L}\p{N}/\s]/gu, " ")
    .trim()
    .split(/\s+/)
    .at(-1);

  return [
    "y",
    "con",
    "pulso",
    "pulsos",
    "puso",
    "mi",
    "es",
    "de",
    "sobre",
    "presion",
  ].includes(lastWord ?? "");
}
