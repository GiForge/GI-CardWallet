package com.example.ui

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(private val onBarcodeDetected: (String, String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient()

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        val formatNum = barcode.format
                        val formatStr = when (formatNum) {
                            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE -> "QR_CODE"
                            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13 -> "EAN_13"
                            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_8 -> "EAN_8"
                            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_128 -> "CODE_128"
                            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_39 -> "CODE_39"
                            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_A -> "UPC_A"
                            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_E -> "UPC_E"
                            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ITF -> "ITF"
                            else -> "CODE_128"
                        }
                        if (rawValue != null) {
                            onBarcodeDetected(rawValue, formatStr)
                            break
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
