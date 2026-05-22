package com.cuidavoz.mobile.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

data class SpeechRecognitionError(
    val code: Int,
    val userMessage: String,
    val errorName: String = errorCodeToName(code),
)

class SpeechRecognitionManager(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var resultCallback: ((String) -> Unit)? = null
    private var partialResultCallback: ((String) -> Unit)? = null
    private var structuredErrorCallback: ((SpeechRecognitionError) -> Unit)? = null
    private var isListening = false
    private var attemptLanguages: List<String> = emptyList()
    private var currentAttemptIndex = 0

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun isOnDeviceRecognitionAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)
        } else {
            false
        }
    }

    fun startListening(
        onResult: (String) -> Unit,
        onPartialResult: (String) -> Unit,
        onError: (SpeechRecognitionError) -> Unit,
    ) {
        val recognitionAvailable = isAvailable()
        val onDeviceRecognitionAvailable = isOnDeviceRecognitionAvailable()
        Log.d(TAG, "[CuidaVoz][SpeechRecognizer] recognitionAvailable=$recognitionAvailable")
        Log.d(TAG, "[CuidaVoz][SpeechRecognizer] onDeviceRecognitionAvailable=$onDeviceRecognitionAvailable")

        if (!recognitionAvailable) {
            onError(
                SpeechRecognitionError(
                    code = SpeechRecognizer.ERROR_CLIENT,
                    userMessage = "Este celular no tiene reconocimiento de voz disponible. Usa los botones.",
                ),
            )
            return
        }

        if (isListening) {
            Log.d(TAG, "[CuidaVoz][SpeechRecognizer] startListening ignored")
            return
        }

        resultCallback = onResult
        partialResultCallback = onPartialResult
        structuredErrorCallback = onError
        attemptLanguages = buildAttemptLanguages()
        currentAttemptIndex = 0
        isListening = true
        startListeningAttempt(currentAttemptIndex)
    }

    fun stopListening() {
        mainHandler.post {
            speechRecognizer?.stopListening()
            isListening = false
        }
    }

    fun cancelListening() {
        mainHandler.post {
            speechRecognizer?.cancel()
            destroyRecognizerInternal()
            isListening = false
            clearCallbacks()
        }
    }

    fun isListening(): Boolean = isListening

    fun destroy() {
        mainHandler.post {
            speechRecognizer?.cancel()
            destroyRecognizerInternal()
            isListening = false
            clearCallbacks()
        }
    }

    private fun startListeningAttempt(attemptIndex: Int) {
        mainHandler.post {
            destroyRecognizerInternal()
            currentAttemptIndex = attemptIndex
            val language = attemptLanguages.getOrNull(attemptIndex) ?: Locale.getDefault().toLanguageTag()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora")
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L)
            }
            Log.d(TAG, "[CuidaVoz][SpeechRecognizer] language=$language")
            if (attemptIndex > 0) {
                Log.d(TAG, "[CuidaVoz][SpeechRecognizer] retryLanguage=$language")
            }
            Log.d(TAG, "[CuidaVoz][SpeechRecognizer] startListening")

            runCatching {
                Log.d(TAG, "[CuidaVoz][SpeechRecognizer] create")
                val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
                speechRecognizer = recognizer
                recognizer.setRecognitionListener(createRecognitionListener(attemptIndex))
                recognizer.startListening(intent)
            }.onFailure {
                isListening = false
                notifyFinalError(
                    SpeechRecognitionError(
                        code = SpeechRecognizer.ERROR_CLIENT,
                        userMessage = "No pude iniciar el micrófono. Intenta otra vez.",
                    ),
                )
            }
        }
    }

    private fun createRecognitionListener(attemptIndex: Int): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "[CuidaVoz][SpeechRecognizer] onReadyForSpeech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "[CuidaVoz][SpeechRecognizer] onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                Log.d(TAG, "[CuidaVoz][SpeechRecognizer] onRmsChanged")
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                Log.d(TAG, "[CuidaVoz][SpeechRecognizer] onEndOfSpeech")
            }

            override fun onError(error: Int) {
                isListening = false
                val errorName = errorCodeToName(error)
                Log.d(TAG, "[CuidaVoz][SpeechRecognizer] onError=$error name=$errorName")
                if (shouldRetryWithFallback(attemptIndex, error)) {
                    val nextIndex = attemptIndex + 1
                    val retryLanguage = attemptLanguages[nextIndex]
                    Log.d(TAG, "[CuidaVoz][SpeechRecognizer] retryLanguage=$retryLanguage")
                    isListening = true
                    startListeningAttempt(nextIndex)
                    return
                }

                notifyFinalError(
                    SpeechRecognitionError(
                        code = error,
                        userMessage = mapErrorToUserMessage(error),
                        errorName = errorName,
                    ),
                )
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                Log.d(TAG, "[CuidaVoz][SpeechRecognizer] onResults received")
                if (text.isBlank()) {
                    notifyFinalError(
                        SpeechRecognitionError(
                            code = SpeechRecognizer.ERROR_NO_MATCH,
                            userMessage = "No pude escucharte bien.",
                        ),
                    )
                    return
                }
                resultCallback?.invoke(text)
                clearCallbacks()
                destroyRecognizerInternal()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (text.isNotBlank()) {
                    Log.d(TAG, "[CuidaVoz][SpeechRecognizer] onPartialResults received")
                    partialResultCallback?.invoke(text)
                }
            }

            override fun onEvent(
                eventType: Int,
                params: Bundle?,
            ) = Unit
        }
    }

    private fun shouldRetryWithFallback(
        attemptIndex: Int,
        error: Int,
    ): Boolean {
        if (attemptIndex >= attemptLanguages.lastIndex) {
            return false
        }
        val languageFallbackError = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
        } else {
            false
        }
        return languageFallbackError || error == SpeechRecognizer.ERROR_NO_MATCH
    }

    private fun buildAttemptLanguages(): List<String> {
        val fallbackLanguage = listOf(
            Locale("es", "US").toLanguageTag(),
            Locale.getDefault().toLanguageTag(),
            Locale("es", "ES").toLanguageTag(),
        ).firstOrNull { it != PRIMARY_LANGUAGE_TAG } ?: Locale("es", "US").toLanguageTag()
        return listOf(PRIMARY_LANGUAGE_TAG, fallbackLanguage).distinct()
    }

    private fun destroyRecognizerInternal() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun notifyFinalError(error: SpeechRecognitionError) {
        destroyRecognizerInternal()
        structuredErrorCallback?.invoke(error)
        clearCallbacks()
    }

    private fun clearCallbacks() {
        resultCallback = null
        partialResultCallback = null
        structuredErrorCallback = null
    }

    private companion object {
        const val TAG = "CuidaVozSpeechRecognizer"
        const val PRIMARY_LANGUAGE_TAG = "es-PE"
    }
}

private fun mapErrorToUserMessage(error: Int): String {
    return when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "No pude escucharte bien. Puedes intentar otra vez o usar los botones."
        SpeechRecognizer.ERROR_CLIENT -> "No pude iniciar el micrófono. Intenta otra vez."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Necesito permiso de micrófono para escucharte."
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER -> "No pude usar la voz en este momento. Usa los botones o intenta otra vez."
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No pude escucharte bien. Puedes intentar otra vez o usar los botones."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Espera un momento y vuelve a intentarlo."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
        SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT,
        SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS -> {
            "Este celular no tiene reconocimiento de voz disponible. Usa los botones."
        }
        else -> "No pude escucharte bien. Puedes intentar otra vez o usar los botones."
    }
}

private fun errorCodeToName(error: Int): String {
    return when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
        SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
        SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
        SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
        SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "ERROR_LANGUAGE_NOT_SUPPORTED"
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "ERROR_LANGUAGE_UNAVAILABLE"
        SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT -> "ERROR_CANNOT_CHECK_SUPPORT"
        SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS -> "ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS"
        else -> "ERROR_UNKNOWN"
    }
}
