package io.github.trojan_gfw.igniter.qrcode;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import java.io.FileDescriptor;
import java.io.IOException;

import io.github.trojan_gfw.igniter.LogHelper;
import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.common.app.BaseAppCompatActivity;
import io.github.trojan_gfw.igniter.common.os.Task;
import io.github.trojan_gfw.igniter.common.os.Threads;

public class ScanQRCodeActivity extends BaseAppCompatActivity {
    private static final String TAG = "ScanQRCodeActivity";
    public static final String KEY_SCAN_CONTENT = "content";
    private static final String INTENT_FROM_GALLERY = "gallery";
    private ActivityResultLauncher<String> mPickPhoto;

    public static Intent create(Context context, boolean readQRCodeFromGallery) {
        return new Intent(context, ScanQRCodeActivity.class).putExtra(INTENT_FROM_GALLERY, readQRCodeFromGallery);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qrcode);
        if (getIntent().getBooleanExtra(INTENT_FROM_GALLERY, false)) {
            mPickPhoto = registerForActivityResult(
                    new ActivityResultContract<String, Uri>() {
                        @NonNull
                        @Override
                        public Intent createIntent(@NonNull Context context, String input) {
                            return new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        }

                        @Override
                        public Uri parseResult(int resultCode, @Nullable Intent intent) {
                            if (intent == null || resultCode != Activity.RESULT_OK) return null;
                            return intent.getData();
                        }
                    },
                    result -> {
                        if (result == null) {
                            showPickPhotoFailedAndFinish();
                            return;
                        }
                        Threads.instance().runOnWorkThread(
                                new Task() {
                                    @Override
                                    public void onRun() {
                                        readQRCodeFromUri(result);
                                    }
                                }
                        );
                    });
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                    ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityResultLauncher<String> requestPermission =
                        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                            if (granted) {
                                scanQRCodeFromGallery();
                            } else {
                                finish();
                            }
                        });
                requestPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                // Reading images from gallery does not require permission READ_EXTERNAL_STORAGE on Android Q or above
                scanQRCodeFromGallery();
            }
        } else {
            scanQRCodeFromCamera();
        }
    }

    private void scanQRCodeFromGallery() {
        mPickPhoto.launch("image/*");
    }

    @Nullable
    @WorkerThread
    private Bitmap resolveBitmapFromUri(@NonNull Uri uri) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4; // use lesser memory to decode the Bitmap

        try (ParcelFileDescriptor parcelFileDescriptor =
                     getContentResolver().openFileDescriptor(uri, "r")) {
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @WorkerThread
    private void readQRCodeFromUri(@NonNull Uri uri) {
        Bitmap qrCodeBitmap = resolveBitmapFromUri(uri);
        if (qrCodeBitmap == null) {
            showPickPhotoFailedAndFinish();
        } else {
            decodeQRCode(qrCodeBitmap);
        }
    }

    @WorkerThread
    private void decodeQRCode(@NonNull Bitmap bitmap) {
        final QRCodeDecoder decoder = new QRCodeDecoder();
        decoder.decode(bitmap, new QRCodeDecoder.OnQRCodeDecodeListener() {
            @Override
            public void onSuccess(@NonNull String result) {
                LogHelper.i(TAG, "onSuccess: " + result);
                if (TextUtils.isEmpty(result)) {
                    showToast(getString(R.string.scan_qr_code_failed, result));
                } else {
                    returnScanResult(result);
                }
                finish();
            }

            @Override
            public void onNoQRCode() {
                LogHelper.i(TAG, "onNoQRCodeDetected");
                setResult(RESULT_CANCELED);
                finish();
            }

            @Override
            public void onError(@NonNull Exception exception) {
                LogHelper.e(TAG, "scan QRCode error: " + exception.getMessage());
                onNoQRCode();
            }

            @Override
            public void onComplete() {
                LogHelper.i(TAG, "scan qr code complete");
                bitmap.recycle();
                decoder.close();
            }
        });
    }

    private void returnScanResult(String result) {
        Intent intent = new Intent();
        intent.putExtra(KEY_SCAN_CONTENT, result);
        setResult(RESULT_OK, intent);
    }

    private void showPickPhotoFailedAndFinish() {
        showToast(getString(R.string.scan_qr_code_failed_to_pick_photo));
        finish();
    }

    private void showToast(String msg) {
        Context context = mContext.getApplicationContext();
        runOnUiThread(() -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
    }

    private void scanQRCodeFromCamera() {
        FragmentManager fm = getSupportFragmentManager();
        ScanQRCodeFragment fragment = (ScanQRCodeFragment) fm.findFragmentByTag(ScanQRCodeFragment.TAG);
        if (fragment == null) {
            fragment = ScanQRCodeFragment.newInstance();
        }
        fm.beginTransaction().replace(R.id.parent_fl, fragment, ScanQRCodeFragment.TAG)
                .commitAllowingStateLoss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPickPhoto != null) {
            mPickPhoto.unregister();
            mPickPhoto = null;
        }
    }
}
