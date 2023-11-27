package io.github.trojan_gfw.igniter.servers;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputEditText;

import io.github.trojan_gfw.igniter.R;

public class SubscribeSettingDialog extends Dialog implements View.OnClickListener {
    private TextInputEditText mSubscribeUrlTiet;
    private OnButtonClickListener mListener;

    public SubscribeSettingDialog(@NonNull Context context) {
        super(context);
        setContentView(R.layout.dialog_subscribe_setting);
        mSubscribeUrlTiet = findViewById(R.id.subscribeLinkUrlTiet);
        findViewById(R.id.subscribeSettingConfirmBtn).setOnClickListener(this);
        findViewById(R.id.subscribeSettingCancelBtn).setOnClickListener(this);
    }

    public void setOnButtonClickListener(OnButtonClickListener listener) {
        mListener = listener;
    }

    public void setSubscribeUrl(String url) {
        mSubscribeUrlTiet.setText(url);
    }

    @Override
    public void onClick(View v) {
        if (mListener == null) return;
        if (v.getId() == R.id.subscribeSettingConfirmBtn){
            Editable editable = mSubscribeUrlTiet.getText();
            String text;
            if (editable != null) {
                text = editable.toString();
            } else {
                text = "";
            }
            mListener.onConfirm(text);
            return;
        }
        if (v.getId() == R.id.subscribeSettingCancelBtn){
            mListener.onCancel();
            return;
        }
    }

    public interface OnButtonClickListener {
        void onConfirm(String url);

        void onCancel();
    }
}
