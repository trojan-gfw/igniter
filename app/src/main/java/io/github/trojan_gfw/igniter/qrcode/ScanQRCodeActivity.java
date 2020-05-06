package io.github.trojan_gfw.igniter.qrcode;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import cn.bingoogolapple.qrcode.core.BarcodeType;
import cn.bingoogolapple.qrcode.zxing.ZXingView;
import io.github.trojan_gfw.igniter.R;

public class ScanQRCodeActivity extends AppCompatActivity implements ZXingView.Delegate {
    public static final String KEY_SCAN_CONTENT = "content";
//    private static final String TAG = "ScanQRCodeActivity";

    public static Intent create(Context context) {
        return new Intent(context, ScanQRCodeActivity.class);
    }

    private ZXingView mZXingView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qrcode);
        mZXingView = findViewById(R.id.zxingview);
        mZXingView.setType(BarcodeType.ONLY_QR_CODE, null);
        mZXingView.setDelegate(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mZXingView.startCamera();
        mZXingView.startSpotAndShowRect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mZXingView.stopSpot();
        mZXingView.stopCamera();
    }

    @Override
    public void onScanQRCodeSuccess(String result) {
        Intent intent = new Intent();
        intent.putExtra(KEY_SCAN_CONTENT, result);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onCameraAmbientBrightnessChanged(boolean isDark) {

    }

    @Override
    public void onScanQRCodeOpenCameraError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), R.string.scan_qr_code_camera_error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
