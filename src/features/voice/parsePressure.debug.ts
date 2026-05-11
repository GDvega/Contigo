/**
 * Dev-only helpers to validate parsePressure. Do not import from production UI.
 *
 * Run: NODE_ENV=development npx tsx src/features/voice/parsePressure.debug.ts
 */

import { parsePressure } from "./parsePressure";

const CASES: { input: string; expectPulse?: number; expectNull?: boolean }[] = [
  { input: "120 sobre 80" },
  { input: "120 sobre 80 pulso 72", expectPulse: 72 },
  { input: "120 sobre 80 y mi pulso es 72", expectPulse: 72 },
  { input: "120 sobre 80 y mi pulso es de 72", expectPulse: 72 },
  {
    input: "impresiones de 120 sobre 80 y mi pulso es de 72",
    expectPulse: 72,
  },
  {
    input: "mi presión es 120 sobre 80 y mi pulso es de 72",
    expectPulse: 72,
  },
  { input: "120/80 pulso 72", expectPulse: 72 },
  { input: "120/80 con 72 de pulso", expectPulse: 72 },
  { input: "120 80 72", expectPulse: 72 },
  { input: "presión 130 85 pulso 70", expectPulse: 70 },
  { input: "999 sobre 80", expectNull: true },
  { input: "120 sobre 80 pulso 999" },
];

export function runParsePressureDebugTests() {
  const isCli =
    typeof process !== "undefined" &&
    (process.argv[1]?.includes("parsePressure.debug") ?? false);
  if (process.env.NODE_ENV !== "development" && !isCli) {
    console.warn("[parsePressure.debug] skipped (not development)");
    return;
  }

  let failed = 0;

  for (const { input, expectPulse, expectNull } of CASES) {
    const got = parsePressure(input);
    let ok = true;

    if (expectNull) {
      ok = got === null;
    } else if (!got) {
      ok = false;
    } else if (expectPulse !== undefined) {
      ok = got.pulse === expectPulse;
    } else {
      ok = got.pulse === undefined;
    }

    if (!ok) {
      failed += 1;
      console.error("[parsePressure.debug] FAIL", { input, expectPulse, expectNull, got });
    } else {
      console.log("[parsePressure.debug] ok", input, "->", got);
    }
  }

  if (failed > 0) {
    console.error(`[parsePressure.debug] ${failed} case(s) failed`);
    process.exitCode = 1;
  } else {
    console.log("[parsePressure.debug] all cases passed");
  }
}

if (typeof process !== "undefined" && process.argv[1]?.includes("parsePressure.debug")) {
  runParsePressureDebugTests();
}
