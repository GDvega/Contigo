"use client";

import { useEffect, useRef, useState } from "react";

type SpeechRecognitionResultLike = {
  readonly 0: {
    readonly transcript: string;
  };
  readonly isFinal: boolean;
};

type SpeechRecognitionEventLike = {
  resultIndex: number;
  results: {
    length: number;
    item(index: number): SpeechRecognitionResultLike;
    [index: number]: SpeechRecognitionResultLike;
  };
};

type SpeechRecognitionLike = {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  maxAlternatives: number;
  start: () => void;
  stop: () => void;
  abort: () => void;
  onresult: ((event: SpeechRecognitionEventLike) => void) | null;
  onstart: (() => void) | null;
  onend: (() => void) | null;
  onerror: ((event: { error?: string }) => void) | null;
};

type SpeechRecognitionConstructor = new () => SpeechRecognitionLike;

type SpeechWindow = Window &
  typeof globalThis & {
    SpeechRecognition?: SpeechRecognitionConstructor;
    webkitSpeechRecognition?: SpeechRecognitionConstructor;
  };

function getSpeechRecognitionConstructor() {
  if (typeof window === "undefined") {
    return undefined;
  }

  const speechWindow = window as SpeechWindow;
  return speechWindow.SpeechRecognition ?? speechWindow.webkitSpeechRecognition;
}

type UseSpeechRecognitionOptions = {
  onTranscript?: (transcript: string) => boolean | void;
  onInterimTranscript?: (transcript: string) => void;
  onFinalTranscript?: (transcript: string) => boolean | void;
  onEnd?: () => void;
  onError?: (error: string) => void;
  /**
   * After the last onresult update, wait this long before emitting the full transcript
   * via onFinalTranscript. Implies continuous listening unless `continuous: false`.
   */
  commandIdleDebounceMs?: number;
  incompleteCommandChecker?: (text: string) => boolean;
  /**
   * Defaults to true when commandIdleDebounceMs is set.
   */
  continuous?: boolean;
};

export function useSpeechRecognition(options: UseSpeechRecognitionOptions = {}) {
  const recognitionRef = useRef<SpeechRecognitionLike | null>(null);
  const optionsRef = useRef(options);

  useEffect(() => {
    optionsRef.current = options;
  });

  const debounceTimerRef = useRef<number | null>(null);
  const latestTranscriptRef = useRef("");
  const commandFinalizeSentRef = useRef(false);

  const [transcript, setTranscript] = useState("");
  const [listening, setListening] = useState(false);
  const [error, setError] = useState("");
  const unsupported = !getSpeechRecognitionConstructor();

  function resetTranscript() {
    setTranscript("");
    setError("");
  }

  function stopListening() {
    if (debounceTimerRef.current) {
      window.clearTimeout(debounceTimerRef.current);
      debounceTimerRef.current = null;
    }
    recognitionRef.current?.stop();
    setListening(false);
  }

  function startListening() {
    const Recognition = getSpeechRecognitionConstructor();

    if (!Recognition) {
      setError("Tu navegador no permite reconocimiento de voz.");
      return;
    }

    recognitionRef.current?.abort();

    const recognition = new Recognition();
    const initialOpts = optionsRef.current;
    const useContinuous =
      initialOpts.continuous ??
      (initialOpts.commandIdleDebounceMs != null ? true : false);

    recognition.lang = "es-PE";
    recognition.continuous = useContinuous;
    recognition.interimResults = true;
    recognition.maxAlternatives = 3;

    recognition.onstart = () => {
      commandFinalizeSentRef.current = false;
      latestTranscriptRef.current = "";
      if (debounceTimerRef.current) {
        window.clearTimeout(debounceTimerRef.current);
        debounceTimerRef.current = null;
      }
      setListening(true);
      setError("");
      setTranscript("");
    };

    recognition.onresult = (event) => {
      let interimTranscript = "";
      let finalTranscript = "";

      for (let index = event.resultIndex; index < event.results.length; index += 1) {
        const result = event.results[index] ?? event.results.item(index);

        if (result.isFinal) {
          finalTranscript += result[0].transcript;
        } else {
          interimTranscript += result[0].transcript;
        }
      }

      const trimmedFinalTranscript = finalTranscript.trim();
      const trimmedInterimTranscript = interimTranscript.trim();
      const nextTranscript = [trimmedFinalTranscript, trimmedInterimTranscript]
        .filter(Boolean)
        .join(" ")
        .trim();

      setTranscript(nextTranscript);
      latestTranscriptRef.current = nextTranscript;

      const currentOpts = optionsRef.current;

      if (trimmedInterimTranscript) {
        currentOpts.onInterimTranscript?.(trimmedInterimTranscript);
      }

      const debounceMs = currentOpts.commandIdleDebounceMs;

      if (debounceMs != null) {
        if (debounceTimerRef.current) {
          window.clearTimeout(debounceTimerRef.current);
        }

        const scheduleIdleFinalize = () => {
          const ms = optionsRef.current.commandIdleDebounceMs ?? debounceMs;
          debounceTimerRef.current = window.setTimeout(() => {
            const text = latestTranscriptRef.current.trim();
            if (!text) {
              return;
            }
            const latestOpts = optionsRef.current;
            if (latestOpts.incompleteCommandChecker?.(text)) {
              scheduleIdleFinalize();
              return;
            }
            if (commandFinalizeSentRef.current) {
              return;
            }
            commandFinalizeSentRef.current = true;
            latestOpts.onFinalTranscript?.(text);
            recognitionRef.current?.stop();
          }, ms);
        };

        scheduleIdleFinalize();
        return;
      }

      const shouldStop = trimmedFinalTranscript
        ? currentOpts.onFinalTranscript?.(trimmedFinalTranscript) ??
          currentOpts.onTranscript?.(nextTranscript)
        : currentOpts.onTranscript?.(nextTranscript);

      if (shouldStop) {
        recognition.stop();
      }
    };

    recognition.onerror = (event) => {
      if (debounceTimerRef.current) {
        window.clearTimeout(debounceTimerRef.current);
        debounceTimerRef.current = null;
      }
      const errorMessages: Record<string, string> = {
        "not-allowed": "Activa el micrófono para usar la voz.",
        "audio-capture": "No encontramos un micrófono disponible.",
        "no-speech": "No escuché nada. Inténtalo otra vez.",
        network: "Hay un problema de red con el reconocimiento de voz.",
        aborted: "La escucha se detuvo.",
      };
      const nextError =
        errorMessages[event.error ?? ""] ??
        "No pudimos escuchar bien. Inténtalo otra vez.";

      setError(nextError);
      optionsRef.current.onError?.(nextError);
      setListening(false);
    };

    recognition.onend = () => {
      if (debounceTimerRef.current) {
        window.clearTimeout(debounceTimerRef.current);
        debounceTimerRef.current = null;
      }

      const endOpts = optionsRef.current;
      if (endOpts.commandIdleDebounceMs != null && !commandFinalizeSentRef.current) {
        const text = latestTranscriptRef.current.trim();
        if (text) {
          commandFinalizeSentRef.current = true;
          endOpts.onFinalTranscript?.(text);
        }
      }

      commandFinalizeSentRef.current = false;
      setListening(false);
      endOpts.onEnd?.();
    };

    recognitionRef.current = recognition;
    recognition.start();
  }

  return {
    transcript,
    listening,
    unsupported,
    error,
    startListening,
    stopListening,
    resetTranscript,
  };
}
