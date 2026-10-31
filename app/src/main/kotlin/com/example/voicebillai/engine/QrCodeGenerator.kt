package com.example.voicebillai.engine

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object QrCodeGenerator {

    /**
     * Constructs the official NPCI UPI Deep-Link string.
     * Format: upi://pay?pa=<VPA>&pn=<PayeeName>&am=<Amount>&cu=INR&tn=<TxnNote>
     */
    fun createUpiUri(
        vpa: String,
        payeeName: String,
        amount: Double,
        transactionNote: String
    ): String {
        val encodedPn = URLEncoder.encode(payeeName.trim(), StandardCharsets.UTF_8.name())
        val encodedTn = URLEncoder.encode(transactionNote.trim(), StandardCharsets.UTF_8.name())
        val formattedAmount = String.format("%.2f", amount)
        return "upi://pay?pa=${vpa.trim()}&pn=$encodedPn&am=$formattedAmount&cu=INR&tn=$encodedTn"
    }

    /**
     * Renders a QR code Bitmap from any text (e.g. UPI URI)
     */
    fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}

