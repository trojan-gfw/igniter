package io.github.trojan_gfw.igniter.qrcode;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import cn.bingoogolapple.qrcode.zxing.QRCodeDecoder;
import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.common.app.BaseAppCompatActivity;
import io.github.trojan_gfw.igniter.common.os.Task;
import io.github.trojan_gfw.igniter.common.os.Threads;

public class ScanQRCodeActivity extends BaseAppCompatActivity {
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
            scanQRCodeFromGallery();
        } else {
            scanQRCodeFromCamera();
        }
    }

    private void scanQRCodeFromGallery() {
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
                        showPickPhotoFailed();
                        finish();
                        return;
                    }
                    Threads.instance().runOnWorkThread(
                            new Task() {
                                @Override
                                public void onRun() {
                                    readQRCodeFromUri(result);
                                    finish();
                                }
                            }
                    );
                });
        mPickPhoto.launch("image/*");
    }

    private void readQRCodeFromUri(@NonNull Uri uri) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri,
                filePathColumn, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            String result = QRCodeDecoder.syncDecodeQRCode(picturePath);
            if (TextUtils.isEmpty(result)) {
                showToast(getString(R.string.scan_qr_code_failed, result));
            } else {
                returnScanResult(result);
            }
        } else {
            showPickPhotoFailed();
        }
    }

    private void returnScanResult(String result) {
        Intent intent = new Intent();
        intent.putExtra(KEY_SCAN_CONTENT, result);
        setResult(RESULT_OK, intent);
    }

    private void showPickPhotoFailed() {
        showToast(getString(R.string.scan_qr_code_failed_to_pick_photo));
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
