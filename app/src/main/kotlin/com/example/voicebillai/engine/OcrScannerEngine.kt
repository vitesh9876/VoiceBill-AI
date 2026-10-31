package com.example.voicebillai.engine

import android.graphics.Bitmap
import com.example.voicebillai.model.BillItem
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object OcrScannerEngine {

    /**
     * Runs multi-script on-device ML Kit OCR on a captured paper list image.
     * Combines Devanagari (Hindi, Marathi, etc.) and Latin/English recognizers.
     */
    suspend fun recognizeTextFromBitmap(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        // Try Devanagari first (handles Hindi + English numbers/latin)
        val devanagariRecognizer = TextRecognition.getClient(
            DevanagariTextRecognizerOptions.Builder().build()
        )

        devanagariRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val text = visionText.text
                if (text.isNotBlank()) {
                    continuation.resume(text)
                } else {
                    // Fallback to Latin recognizer
                    val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    latinRecognizer.process(inputImage)
                        .addOnSuccessListener { latinText ->
                            continuation.resume(latinText.text)
                        }
                        .addOnFailureListener {
                            continuation.resume("")
                        }
                }
            }
            .addOnFailureListener {
                val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                latinRecognizer.process(inputImage)
                    .addOnSuccessListener { latinText ->
                        continuation.resume(latinText.text)
                    }
                    .addOnFailureListener {
                        continuation.resume("")
                    }
            }
    }

    /**
     * Parses recognized OCR text lines into structured BillItem candidates.
     */
    fun parseOcrTextToItems(rawText: String): List<BillItem> {
        val lines = rawText.split("\n", ";", ",")
            .map { it.trim() }
            .filter { it.length > 2 }

        val items = mutableListOf<BillItem>()
        for (line in lines) {
            val result = VoiceParserEngine.parseUtterance(line)
            if (result is VoiceActionResult.AddItems) {
                items.addAll(result.items.map { it.copy(inputSource = "ocr") })
            }
        }
        return items
    }
}

