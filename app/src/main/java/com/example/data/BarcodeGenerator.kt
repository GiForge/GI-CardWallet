package com.example.data

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object BarcodeGenerator {
    fun generateBarcode(content: String, format: String, width: Int = 800, height: Int = 300): Bitmap? {
        if (content.isEmpty()) return null
        val barcodeFormat = when (format.uppercase()) {
            "QR_CODE" -> BarcodeFormat.QR_CODE
            "EAN_13" -> BarcodeFormat.EAN_13
            "EAN_8" -> BarcodeFormat.EAN_8
            "UPC_A" -> BarcodeFormat.UPC_A
            "UPC_E" -> BarcodeFormat.UPC_E
            "CODE_128" -> BarcodeFormat.CODE_128
            "CODE_39" -> BarcodeFormat.CODE_39
            "ITF" -> BarcodeFormat.ITF
            else -> BarcodeFormat.CODE_128
        }
        
        // Adjust widths for 2D vs 1D
        val finalWidth = if (barcodeFormat == BarcodeFormat.QR_CODE) height else width
        
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                barcodeFormat,
                finalWidth,
                height
            )
            val bitmap = Bitmap.createBitmap(finalWidth, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until finalWidth) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
