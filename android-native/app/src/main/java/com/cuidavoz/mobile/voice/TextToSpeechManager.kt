package com.cuidavoz.mobile.voice

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

class TextToSpeechManager(
    context: Context,
) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var textToSpeech: TextToSpeech? = null
    private var isReady = false
    private var isSpeaking = false
    private var onDoneListener: ((Boolean) -> Unit)? = null
    private var isInitializing = false
    private var pendingUtterance: PendingUtterance? = null

    init {
        ensureInitialized()
    }

    override fun onInit(status: Int) {
        isInitializing = false
        if (status != TextToSpeech.SUCCESS) {
            Log.e(TAG, "No se pudo inicializar TextToSpeech")
            isReady = false
            pendingUtterance?.onDone?.invoke(false)
            pendingUtterance = null
            return
        }

        val tts = textToSpeech ?: return
        val locale = resolveSpanishLocale(tts)
        val result = tts.setLanguage(locale)
        isReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        tts.setSpeechRate(0.92f)
        tts.setPitch(1.0f)
        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                    Log.d(TAG, "Comenzando locucion")
                }

                override fun onDone(utteranceId: String?) {
                    mainHandler.post {
                        isSpeaking = false
                        onDoneListener?.invoke(true)
                        onDoneListener = null
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    mainHandler.post {
                        isSpeaking = false
                        Log.e(TAG, "Error al reproducir locucion")
                        onDoneListener?.invoke(false)
                        onDoneListener = null
                    }
                }

                override fun onError(
                    utteranceId: String?,
                    errorCode: Int,
                ) {
                    mainHandler.post {
                        isSpeaking = false
                        Log.e(TAG, "Error al reproducir locucion: $errorCode")
                        onDoneListener?.invoke(false)
                        onDoneListener = null
                    }
                }
            },
        )
        Log.d(TAG, "TextToSpeech listo con locale ${locale.toLanguageTag()}")
        if (!isReady) {
            pendingUtterance?.onDone?.invoke(false)
            pendingUtterance = null
            return
        }
        pendingUtterance?.let { utterance ->
            pendingUtterance = null
            speak(utterance.text, utterance.onDone)
        }
    }

    fun speak(
        text: String,
        onDone: ((Boolean) -> Unit)? = null,
    ): Boolean {
        val tts = textToSpeech
        if (!isReady || tts == null) {
            Log.w(TAG, "TextToSpeech no esta listo. Texto en cola.")
            ensureInitialized()
            pendingUtterance = PendingUtterance(text = text, onDone = onDone)
            return true
        }

        stop()
        onDoneListener = onDone
        val utteranceId = UUID.randomUUID().toString()
        val result = tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            Bundle(),
            utteranceId,
        )
        if (result == TextToSpeech.ERROR) {
            isSpeaking = false
            Log.e(TAG, "No se pudo hablar el texto")
            onDoneListener = null
            onDone?.invoke(false)
            return false
        }
        return true
    }

    fun speakRepeated(
        text: String,
        times: Int = 2,
        onDone: ((Boolean) -> Unit)? = null,
    ): Boolean {
        val safeTimes = times.coerceIn(1, 3)
        val repeatedText = List(safeTimes) { text }.joinToString(". ")
        return speak(repeatedText, onDone)
    }

    fun stop() {
        textToSpeech?.stop()
        isSpeaking = false
        onDoneListener = null
        pendingUtterance = null
    }

    fun isSpeaking(): Boolean = isSpeaking

    fun shutdown() {
        mainHandler.post {
            stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isReady = false
            isInitializing = false
        }
    }

    private fun ensureInitialized() {
        if (textToSpeech != null || isInitializing) {
            return
        }
        isInitializing = true
        mainHandler.post {
            if (textToSpeech == null) {
                textToSpeech = TextToSpeech(appContext, this)
            }
        }
    }

    private fun resolveSpanishLocale(tts: TextToSpeech): Locale {
        val candidates = listOf(
            Locale("es", "PE"),
            Locale("es", "ES"),
            Locale("es"),
        )
        return candidates.firstOrNull { candidate ->
            val result = tts.isLanguageAvailable(candidate)
            result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        } ?: Locale.getDefault()
    }

    private companion object {
        const val TAG = "CuidaVozTTS"
    }
}

private data class PendingUtterance(
    val text: String,
    val onDone: ((Boolean) -> Unit)?,
)
