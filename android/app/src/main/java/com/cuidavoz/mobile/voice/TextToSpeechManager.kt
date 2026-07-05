package com.cuidavoz.mobile.voice

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.cuidavoz.mobile.util.ContigoLog
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

    override fun onInit(status: Int) {
        isInitializing = false
        if (status != TextToSpeech.SUCCESS) {
            ContigoLog.e(TAG, "No se pudo inicializar TextToSpeech")
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
                    ContigoLog.d(TAG, "Comenzando locucion")
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
                        ContigoLog.e(TAG, "Error al reproducir locucion")
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
                        ContigoLog.e(TAG, "Error al reproducir locucion: $errorCode")
                        onDoneListener?.invoke(false)
                        onDoneListener = null
                    }
                }
            },
        )
        configureForMedicationReminders(tts)
        ContigoLog.d(TAG, "TextToSpeech listo con locale ${locale.toLanguageTag()}")
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

    /** Volumen y prioridad adecuados para recordatorios de medicación (accesibilidad). */
    fun configureForMedicationReminders() {
        textToSpeech?.let(::configureForMedicationReminders)
    }

    fun speak(
        text: String,
        onDone: ((Boolean) -> Unit)? = null,
    ): Boolean {
        val tts = textToSpeech
        if (!isReady || tts == null) {
            ContigoLog.w(TAG, "TextToSpeech no esta listo. Texto en cola.")
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
            ContigoLog.e(TAG, "No se pudo hablar el texto")
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

    private fun configureForMedicationReminders(tts: TextToSpeech) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
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
        const val TAG = "ContigoTTS"
    }
}

private data class PendingUtterance(
    val text: String,
    val onDone: ((Boolean) -> Unit)?,
)
