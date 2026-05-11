import {
  ExpoSpeechRecognitionModule,
  RecognizerIntentExtraLanguageModel,
  type ExpoSpeechRecognitionErrorCode,
} from "expo-speech-recognition";
import * as Speech from "expo-speech";

type ListenOptions = {
  onPartial?: (transcript: string) => void;
  timeoutMs?: number;
};

type VoiceFailureCode =
  | "microphone_permission_denied"
  | "speech_service_unavailable"
  | "recognition_failed";

class VoiceError extends Error {
  code: VoiceFailureCode;

  constructor(code: VoiceFailureCode) {
    super(code);
    this.name = "VoiceError";
    this.code = code;
  }
}

let activeSubscriptions: Array<{ remove: () => void }> = [];

function clearSubscriptions() {
  for (const subscription of activeSubscriptions) {
    subscription.remove();
  }

  activeSubscriptions = [];
}

function extractTranscript(results: Array<{ transcript: string }> | undefined) {
  return results?.[0]?.transcript?.trim() ?? "";
}

function mapRecognitionError(error: ExpoSpeechRecognitionErrorCode) {
  if (error === "not-allowed") {
    return new VoiceError("microphone_permission_denied");
  }

  if (error === "service-not-allowed" || error === "language-not-supported") {
    return new VoiceError("speech_service_unavailable");
  }

  return new VoiceError("recognition_failed");
}

export async function requestMicrophonePermission() {
  const result = await ExpoSpeechRecognitionModule.requestPermissionsAsync();
  return result.granted;
}

export async function speakAsync(text: string) {
  await Speech.stop();

  return new Promise<void>((resolve) => {
    Speech.speak(text, {
      language: "es-PE",
      onDone: resolve,
      onStopped: resolve,
      onError: () => resolve(),
    });
  });
}

export async function stopListening() {
  try {
    ExpoSpeechRecognitionModule.stop();
  } catch {
    // Recognition may already be stopped.
  }
}

export async function destroyVoice() {
  clearSubscriptions();

  try {
    ExpoSpeechRecognitionModule.abort();
  } catch {
    // The recognizer may already be inactive.
  }
}

export async function listenOnce({
  onPartial,
  timeoutMs = 9000,
}: ListenOptions = {}) {
  const hasPermission = await requestMicrophonePermission();
  if (!hasPermission) {
    throw new VoiceError("microphone_permission_denied");
  }

  if (!ExpoSpeechRecognitionModule.isRecognitionAvailable()) {
    throw new VoiceError("speech_service_unavailable");
  }

  clearSubscriptions();

  return new Promise<string>((resolve, reject) => {
    let settled = false;
    let latestTranscript = "";
    let endTimer: ReturnType<typeof setTimeout> | null = null;

    const clearEndTimer = () => {
      if (endTimer) {
        clearTimeout(endTimer);
        endTimer = null;
      }
    };

    const cleanup = () => {
      clearTimeout(timeout);
      clearEndTimer();
      clearSubscriptions();
    };

    const finish = (value: string) => {
      if (settled) {
        return;
      }

      settled = true;
      cleanup();

      try {
        ExpoSpeechRecognitionModule.stop();
      } catch {
        // Safe to ignore if recognition already ended.
      }

      resolve(value.trim());
    };

    const fail = (error: unknown) => {
      if (settled) {
        return;
      }

      settled = true;
      cleanup();

      try {
        ExpoSpeechRecognitionModule.abort();
      } catch {
        // Safe to ignore if recognition already ended.
      }

      reject(error);
    };

    const timeout = setTimeout(() => {
      if (latestTranscript) {
        finish(latestTranscript);
        return;
      }

      fail(new VoiceError("recognition_failed"));
    }, timeoutMs);

    activeSubscriptions = [
      ExpoSpeechRecognitionModule.addListener("result", (event) => {
        latestTranscript = extractTranscript(event.results) || latestTranscript;
        onPartial?.(latestTranscript);

        if (event.isFinal && latestTranscript) {
          finish(latestTranscript);
        }
      }),
      ExpoSpeechRecognitionModule.addListener("end", () => {
        clearEndTimer();
        endTimer = setTimeout(() => {
          if (latestTranscript) {
            finish(latestTranscript);
            return;
          }

          fail(new VoiceError("recognition_failed"));
        }, 600);
      }),
      ExpoSpeechRecognitionModule.addListener("error", (event) => {
        if (event.error === "no-speech" || event.error === "speech-timeout") {
          fail(new VoiceError("recognition_failed"));
          return;
        }

        fail(mapRecognitionError(event.error));
      }),
    ];

    try {
      ExpoSpeechRecognitionModule.start({
        lang: "es-PE",
        interimResults: true,
        maxAlternatives: 1,
        continuous: false,
        addsPunctuation: false,
        androidIntentOptions: {
          EXTRA_LANGUAGE_MODEL:
            RecognizerIntentExtraLanguageModel.LANGUAGE_MODEL_FREE_FORM,
          EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS: timeoutMs,
          EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS: 1500,
        },
      });
    } catch (error) {
      fail(error instanceof Error ? error : new VoiceError("recognition_failed"));
    }
  });
}

export function isVoiceError(error: unknown): error is VoiceError {
  return error instanceof VoiceError;
}
