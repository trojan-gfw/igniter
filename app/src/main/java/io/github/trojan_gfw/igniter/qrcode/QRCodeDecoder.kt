package io.github.trojan_gfw.igniter.qrcode

import android.graphics.Bitmap
import android.text.TextUtils
import androidx.annotation.NonNull
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class QRCodeDecoder {
    private val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder()
            .apply { setBarcodeFormats(Barcode.FORMAT_QR_CODE) }.build())

    fun decode(bitmap: Bitmap, listener: OnQRCodeDecodeListener) {
        scanner.process(InputImage.fromBitmap(bitmap, 0)).addOnSuccessListener { barcodes ->
            val barcode = barcodes.firstOrNull {
                it.rawValue != null && !TextUtils.isEmpty(it.rawValue!!.trim())
            }
            if (null == barcode) {
                listener.onNoQRCodeDetected()
            } else {
                listener.onSuccess(barcode.rawValue!!)
            }
        }.addOnFailureListener { exception ->
            listener.onError(exception)
        }.addOnCompleteListener {
            listener.onComplete()
        }.addOnCanceledListener { listener.onNoQRCodeDetected() }
    }

    fun close() {
        scanner.close()
    }

    interface OnQRCodeDecodeListener {
        fun onSuccess(@NonNull result: String)
        fun onNoQRCodeDetected()
        fun onError(@NonNull exception: Exception)
        fun onComplete()
    }
}