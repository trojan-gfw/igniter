package io.github.trojan_gfw.igniter.settings;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;

import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.common.utils.DisplayUtils;

public class InputEntryView extends FrameLayout {
    private TextInputEditText mEditText;
    private Listener mListener;

    public InputEntryView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public InputEntryView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public InputEntryView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mEditText = new TextInputEditText(context);
        int width = (int) context.getResources().getDimension(R.dimen.settings_dns_input_width);
        mEditText.setLayoutParams(new LayoutParams(width, LayoutParams.WRAP_CONTENT));
        ImageView deleteIv = new ImageView(context);
        deleteIv.setImageResource(R.drawable.icon_remove);
        int size = (int) context.getResources().getDimension(R.dimen.settings_dns_delete_btn_size);
        LayoutParams lp = new LayoutParams(size, size);
        lp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        deleteIv.setLayoutParams(lp);
        deleteIv.setScaleType(ImageView.ScaleType.FIT_XY);
        addView(mEditText);
        addView(deleteIv);
        deleteIv.setOnClickListener(v-> {
            if (mListener != null) {
                mListener.onDelete(this);
            }
        });
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                mEditText.setError("");
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    public void setText(CharSequence text) {
        mEditText.setText(text);
        mEditText.setError(null);
    }

    @NonNull
    public CharSequence getText() {
        CharSequence cs = mEditText.getText();
        return cs == null ? "" : cs;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setError(String error) {
        mEditText.setError(error);
    }

    public interface Listener {
        void onDelete(InputEntryView view);
    }
}