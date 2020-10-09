package io.github.trojan_gfw.igniter.qrcode;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.bingoogolapple.qrcode.core.BarcodeType;
import cn.bingoogolapple.qrcode.zxing.ZXingView;
import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.common.app.BaseFragment;

public class ScanQRCodeFragment extends BaseFragment implements ZXingView.Delegate {
    public static final String TAG = "ScanQRCodeFragment";
    private static final String KEY_SCAN_CONTENT = ScanQRCodeActivity.KEY_SCAN_CONTENT;
    private ZXingView mZXingView;

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
        mZXingView = view.findViewById(R.id.zxingview);
        mZXingView.setType(BarcodeType.ONLY_QR_CODE, null);
        mZXingView.setDelegate(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mZXingView.startCamera();
        mZXingView.startSpotAndShowRect();
    }

    @Override
    public void onPause() {
        super.onPause();
        mZXingView.stopSpot();
        mZXingView.stopCamera();
    }

    @Override
    public void onScanQRCodeSuccess(String result) {
        Activity activity = requireActivity();
        activity.setResult(Activity.RESULT_OK, new Intent().putExtra(KEY_SCAN_CONTENT, result));
        activity.finish();
    }

    @Override
    public void onCameraAmbientBrightnessChanged(boolean isDark) {
    }

    @Override
    public void onScanQRCodeOpenCameraError() {
        final Context context = mContext.getApplicationContext();
        runOnUiThread(() -> Toast.makeText(context, R.string.scan_qr_code_camera_error, Toast.LENGTH_SHORT).show());
    }
}
