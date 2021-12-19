package io.github.trojan_gfw.igniter.qrcode;

import android.graphics.Bitmap;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.Optional;

public class QRCodeDecoder {
    private final BarcodeScanner mScanner;

    public QRCodeDecoder() {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE).build();
        mScanner = BarcodeScanning.getClient(options);
    }

    public void decode(@NonNull Bitmap bitmap, @NonNull OnQRCodeDecodeListener listener) {
        mScanner.process(InputImage.fromBitmap(bitmap, 0)).addOnSuccessListener(barcodes -> {
            Optional<Barcode> qrCode = barcodes.stream().filter(barcode -> {
                String rawValue = barcode.getRawValue();
                return rawValue != null && !TextUtils.isEmpty(rawValue.trim());
            }).findFirst();
            if (qrCode.isPresent()) {
                listener.onSuccess(qrCode.get().getRawValue());
            } else {
                listener.onNoQRCode();
            }
        }).addOnCanceledListener(listener::onNoQRCode)
                .addOnFailureListener(listener::onError)
                .addOnCompleteListener(barcodes -> listener.onComplete());
    }

    public void close() {
        mScanner.close();
    }

    public interface OnQRCodeDecodeListener {
        void onSuccess(String qrCode);

        void onError(Exception e);

        void onNoQRCode();

        void onComplete();
    }
}
