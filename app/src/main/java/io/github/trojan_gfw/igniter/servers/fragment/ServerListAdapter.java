package io.github.trojan_gfw.igniter.servers.fragment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.TrojanConfig;
import io.github.trojan_gfw.igniter.servers.data.TrojanConfigWrapper;

public class ServerListAdapter extends RecyclerView.Adapter<ViewHolder> {
    private final LayoutInflater mInflater;
    private final List<TrojanConfigWrapper> mData;
    private OnItemClickListener mOnItemClickListener;
    private boolean mBatchDeleteMode;

    public ServerListAdapter(Context context, List<TrojanConfig> data) {
        super();
        this.mData = new ArrayList<>(data.size());
        for (TrojanConfig config : data) {
            mData.add(new TrojanConfigWrapper(config));
        }
        mInflater = LayoutInflater.from(context);
    }

    public void setAllSelected(boolean selected) {
        for (TrojanConfigWrapper datum : mData) {
            datum.setSelected(selected);
        }
        notifyItemRangeChanged(0, mData.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        ViewHolder vh = new ViewHolder(mInflater.inflate(R.layout.item_server, viewGroup, false));
        vh.bindListener(mOnItemClickListener);
        return vh;
    }

    public List<TrojanConfig> getData() {
        return new ArrayList<>(mData); // avoid outside modification of mData.
    }

    public void deleteServers(Collection<TrojanConfig> toDeleteConfigs) {
        for (int i = mData.size() - 1; i >= 0; i--) {
            if (toDeleteConfigs.contains(mData.get(i))) {
                mData.remove(i);
            }
        }
        notifyDataSetChanged();
    }

    public void setBatchDeleteMode(boolean enable) {
        mBatchDeleteMode = enable;
        notifyItemRangeChanged(0, mData.size());
    }

    public void replaceData(List<TrojanConfig> data) {
        mData.clear();
        for (TrojanConfig config: data) {
            mData.add(new TrojanConfigWrapper(config));
        }
        notifyDataSetChanged();
    }

    public void removeItemOnPosition(int pos) {
        mData.remove(pos);
        notifyItemRemoved(pos);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.bindData(mData.get(i), mBatchDeleteMode);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemSelected(TrojanConfig config, int pos);
        void onItemBatchSelected(TrojanConfig config, int pos, boolean checked);
    }
}

class ViewHolder extends RecyclerView.ViewHolder {
    private TrojanConfigWrapper mConfig;
    private TextView mRemoteAddrTv;
    private CheckBox mCheckBox;
    private boolean mBatchDeleteMode;
    private ServerListAdapter.OnItemClickListener itemClickListener;
    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = ((checkBox, isChecked) -> {
        if (itemClickListener != null) {
            mConfig.setSelected(isChecked);
            itemClickListener.onItemBatchSelected(mConfig, getBindingAdapterPosition(), isChecked);
        }
    });

    public ViewHolder(@NonNull final View itemView) {
        super(itemView);
        mRemoteAddrTv = itemView.findViewById(R.id.serverAddrTv);
        itemView.setOnClickListener(v -> {
            if (itemClickListener != null && !mBatchDeleteMode) {
                itemClickListener.onItemSelected(mConfig.getDelegate(), getBindingAdapterPosition());
            }
        });
        mCheckBox = itemView.findViewById(R.id.serverCb);
    }

    public void bindData(TrojanConfigWrapper config, boolean batchDeleteMode) {
        this.mConfig = config;
        mRemoteAddrTv.setText(config.getRemoteServerRemark());
        mCheckBox.setVisibility(batchDeleteMode ? View.VISIBLE : View.GONE);
        mCheckBox.setOnCheckedChangeListener(null);
        mCheckBox.setChecked(config.isSelected());
        mCheckBox.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mBatchDeleteMode = batchDeleteMode;
    }

    public void bindListener(ServerListAdapter.OnItemClickListener listener) {
        itemClickListener = listener;
    }
}
