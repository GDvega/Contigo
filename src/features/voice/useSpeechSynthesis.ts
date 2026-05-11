"use client";

export function useSpeechSynthesis() {
  function createUtterance(text: string) {
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = "es-PE";
    utterance.rate = 0.9;

    const spanishVoice = window.speechSynthesis
      .getVoices()
      .find((voice) => voice.lang.startsWith("es"));

    if (spanishVoice) {
      utterance.voice = spanishVoice;
    }

    return utterance;
  }

  function speak(text: string) {
    if (typeof window === "undefined" || !("speechSynthesis" in window)) {
      return;
    }

    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(createUtterance(text));
  }

  function speakAsync(text: string) {
    if (typeof window === "undefined" || !("speechSynthesis" in window)) {
      return Promise.resolve();
    }

    window.speechSynthesis.cancel();

    return new Promise<void>((resolve) => {
      const utterance = createUtterance(text);
      utterance.onend = () => resolve();
      utterance.onerror = () => resolve();

      window.speechSynthesis.speak(utterance);
    });
  }

  return { speak, speakAsync };
}
