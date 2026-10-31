package com.example.voicebillai.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class SoundboxTtsService(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.language = Locale("en", "IN")
            }
        }
    }

    fun setLanguage(langTag: String) {
        if (!isInitialized) return
        val loc = when {
            langTag.startsWith("te", ignoreCase = true) -> Locale("te", "IN")
            langTag.startsWith("hi", ignoreCase = true) -> Locale("hi", "IN")
            langTag.startsWith("ta", ignoreCase = true) -> Locale("ta", "IN")
            langTag.startsWith("kn", ignoreCase = true) -> Locale("kn", "IN")
            langTag.startsWith("mr", ignoreCase = true) -> Locale("mr", "IN")
            else -> Locale("en", "IN")
        }
        val result = tts?.setLanguage(loc)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.language = Locale.ENGLISH
        }
    }

    fun speak(text: String) {
        if (!isInitialized || text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SoundboxFeedback_${System.currentTimeMillis()}")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}

