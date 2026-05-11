export type ParsedMedicationConfirmation = {
  confirmedIntent: true;
  transcript: string;
};

function normalize(text: string) {
  return text
    .toLowerCase()
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .replace(/[^\p{L}\p{N}\s]/gu, " ")
    .replace(/\s+/g, " ")
    .trim();
}

export function parseMedicationConfirmation(
  text: string
): ParsedMedicationConfirmation | null {
  const normalized = normalize(text);

  if (!normalized || normalized.includes("no se")) {
    return null;
  }

  const strongPhrases = [
    "ya tome",
    "ya la tome",
    "la tome",
    "tome mi pastilla",
    "tome la pastilla",
    "tome mi medicina",
    "tome mi medicamento",
    "tome mi pildora",
    "tome el medicamento",
    "ya tome losartan",
    "si ya tome",
    "ya tome mi",
  ];

  if (!strongPhrases.some((phrase) => normalized.includes(phrase))) {
    return null;
  }

  return {
    confirmedIntent: true,
    transcript: text,
  };
}
