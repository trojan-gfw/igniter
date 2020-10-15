package io.github.trojan_gfw.igniter.qrcode;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.trojan_gfw.igniter.LogHelper;
import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.common.app.BaseFragment;

public class ScanQRCodeFragment extends BaseFragment implements ImageAnalysis.Analyzer {
    public static final String TAG = "ScanQRCodeFragment";
    private static final int DEFAULT_IMAGE_ANALYSIS_WIDTH = 1080;
    private static final int DEFAULT_IMAGE_ANALYSIS_HEIGHT = 1920;
    private static final String KEY_SCAN_CONTENT = ScanQRCodeActivity.KEY_SCAN_CONTENT;
    private BarcodeScanner mScanner;
    private Executor mMainExecutor;
    private ExecutorService mCameraExecutor;
    private ImageAnalysis mImageAnalysis;

    public static ScanQRCodeFragment newInstance() {
        return new ScanQRCodeFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan_qr_code, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prepareForScanning();
        startCamera();
    }

    private void prepareForScanning() {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE).build();
        mScanner = BarcodeScanning.getClient(options);
        mMainExecutor = ContextCompat.getMainExecutor(mContext);
        mCameraExecutor = Executors.newSingleThreadExecutor();
    }

    private ImageAnalysis getImageAnalysis() {
        if (mImageAnalysis == null) {
            mImageAnalysis = new ImageAnalysis.Builder()
                    .setTargetResolution(new Size(DEFAULT_IMAGE_ANALYSIS_WIDTH, DEFAULT_IMAGE_ANALYSIS_HEIGHT))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();
            mImageAnalysis.setAnalyzer(mCameraExecutor, this);
        }
        return mImageAnalysis;
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(mContext);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                PreviewView previewView = findViewById(R.id.previewView);
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA,
                        getImageAnalysis(), preview);
            } catch (ExecutionException | InterruptedException e) {
                LogHelper.e(TAG, "Bind camera failed: " + e.getMessage());
                Toast.makeText(mContext.getApplicationContext(), R.string.scan_qr_code_camera_error, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                finishActivity();
            }
        }, mMainExecutor);
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        @SuppressLint("UnsafeExperimentalUsageError") Image image = imageProxy.getImage();
        if (image == null) {
            imageProxy.close();
            return;
        }
        mScanner.process(InputImage.fromMediaImage(image, 0)).addOnSuccessListener(barcodes -> {
            barcodes.stream().filter(barcode -> {
                String rawValue = barcode.getRawValue();
                return rawValue != null && !TextUtils.isEmpty(rawValue.trim());
            }).findFirst().ifPresent(barcode -> {
                mImageAnalysis.clearAnalyzer();
                returnScanResult(barcode.getRawValue());
            });
        }).addOnCanceledListener(() -> {
            mImageAnalysis.clearAnalyzer();
            returnEmptyResult();
        })
                .addOnFailureListener(exception -> {
                    LogHelper.i(TAG, "onFailed " + exception.getMessage());
                    mImageAnalysis.clearAnalyzer();
                    imageProxy.close();
                    showScanQRCodeError();
                    returnEmptyResult();
                }).addOnCompleteListener(barcodes -> {
            imageProxy.close();
        });
    }

    private void returnScanResult(String qrCode) {
        Activity activity = requireActivity();
        activity.setResult(Activity.RESULT_OK, new Intent().putExtra(KEY_SCAN_CONTENT, qrCode));
        activity.finish();
    }

    private void returnEmptyResult() {
        Activity activity = requireActivity();
        activity.setResult(Activity.RESULT_CANCELED);
        activity.finish();
    }

    private void showScanQRCodeError() {
        final Context context = mContext.getApplicationContext();
        runOnUiThread(() -> Toast.makeText(context, R.string.scan_qr_code_camera_error,
                Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCameraExecutor != null) {
            mCameraExecutor.shutdown();
            mCameraExecutor = null;
        }
        mMainExecutor = null;
        mScanner = null;
        mImageAnalysis = null;
    }
}
