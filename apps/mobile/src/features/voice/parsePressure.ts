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
  diecisiete: 17,
  dieciocho: 18,
  diecinueve: 19,
  veinte: 20,
  veintiuno: 21,
  veintiun: 21,
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

const pulseKeywordPattern =
  "(?:frecuencia\\s+cardiaca|pulsaciones|pulsacion|pulso|pulsos|puso|frecuencia|latidos|ritmo)";
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
      if (foundNumber) break;
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

function isValidPulse(pulse: number) {
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
    return pulse !== undefined && isValidPulse(pulse) ? pulse : undefined;
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
  const numbers = normalized.match(/\b\d{2,3}\b/g)?.map(Number) ?? [];

  const numericMatch = normalized.match(
    /\b(\d{2,3})\s*(?:\/|sobre|con)\s*(\d{2,3})\b/
  );

  if (numericMatch) {
    const systolic = Number(numericMatch[1]);
    const diastolic = Number(numericMatch[2]);
    const thirdAfterPair =
      numbers.length >= 3 &&
      numbers[0] === systolic &&
      numbers[1] === diastolic
        ? numbers[2]
        : undefined;

    return withOptionalPulse(systolic, diastolic, explicitPulse ?? thirdAfterPair);
  }

  if (numbers.length >= 2) {
    const [systolic, diastolic, fallbackPulse] = numbers;
    return withOptionalPulse(systolic, diastolic, explicitPulse ?? fallbackPulse);
  }

  const wordMatch = normalized.match(
    new RegExp(
      `(?:presion es|presion|tengo|es)?\\s*([a-z\\s]+?)\\s+sobre\\s+([a-z\\s]+?)(?:\\s+(?:y\\s+mi\\s+)?${pulseKeywordPattern}\\b|$)`
    )
  );

  if (!wordMatch) {
    return null;
  }

  const systolic = parseNumberWords(wordMatch[1]);
  const diastolic = parseNumberWords(wordMatch[2]);

  if (systolic === undefined || diastolic === undefined) {
    return null;
  }

  return withOptionalPulse(systolic, diastolic, explicitPulse);
}
