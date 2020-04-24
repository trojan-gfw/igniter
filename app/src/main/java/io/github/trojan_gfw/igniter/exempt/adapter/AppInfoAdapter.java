package io.github.trojan_gfw.igniter.exempt.adapter;

import android.content.res.Resources;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.exempt.data.AppInfo;

public class AppInfoAdapter extends RecyclerView.Adapter<AppInfoAdapter.ViewHolder> {
    private final List<AppInfo> mData = new ArrayList<>();
    private OnItemOperationListener mOnItemOperationListener;
    private final Rect mIconBound = new Rect();

    public AppInfoAdapter() {
        super();
        final int size = Resources.getSystem().getDimensionPixelSize(android.R.dimen.app_icon_size);
        mIconBound.right = size;
        mIconBound.bottom = size;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_app_info, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        if (i != RecyclerView.NO_POSITION) {
            viewHolder.bindData(mData.get(i));
        }
    }

    public void removeData(int position) {
        mData.remove(position);
        notifyItemRemoved(position);
    }

    public void refreshData(List<AppInfo> data) {
        mData.clear();
        mData.addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void setOnItemOperationListener(OnItemOperationListener onItemOperationListener) {
        mOnItemOperationListener = onItemOperationListener;
    }

    public interface OnItemOperationListener {
        void onToggle(boolean exempt, AppInfo appInfo, int position);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements CompoundButton.OnCheckedChangeListener {
        private TextView mNameTv;
        private Switch mExemptSwitch;
        private AppInfo mCurrentInfo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            mNameTv = itemView.findViewById(R.id.appNameTv);
            TextViewCompat.setAutoSizeTextTypeWithDefaults(mNameTv, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
            mExemptSwitch = itemView.findViewById(R.id.appExemptSwitch);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (mOnItemOperationListener != null) {
                mOnItemOperationListener.onToggle(isChecked, mCurrentInfo, getAdapterPosition());
            }
        }

        void bindData(AppInfo appInfo) {
            mCurrentInfo = appInfo;
            mNameTv.setText(appInfo.getAppName());
            appInfo.getIcon().setBounds(mIconBound);
            mNameTv.setCompoundDrawables(appInfo.getIcon(), null, null, null);
            mExemptSwitch.setOnCheckedChangeListener(null);
            mExemptSwitch.setChecked(appInfo.isExempt());
            mExemptSwitch.setOnCheckedChangeListener(this);
        }
    }
}
