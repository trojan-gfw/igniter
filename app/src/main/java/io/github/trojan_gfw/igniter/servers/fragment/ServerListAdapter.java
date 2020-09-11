package io.github.trojan_gfw.igniter.servers.fragment;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.TrojanConfig;
import io.github.trojan_gfw.igniter.servers.ItemVerticalMoveCallback;
import io.github.trojan_gfw.igniter.servers.data.TrojanConfigWrapper;
import io.github.trojan_gfw.igniter.servers.data.ServerListDataManager;

public class ServerListAdapter extends RecyclerView.Adapter<ViewHolder> implements ItemVerticalMoveCallback.ItemTouchHelperContract {
    private final static String TAG = "ServerListAdapter";

    private final LayoutInflater mInflater;
    private final List<TrojanConfigWrapper> mData;
    private final List<Float> mPingDelayTimeView;
    private OnItemClickListener mOnItemClickListener;
    private boolean mBatchDeleteMode;

    public ServerListAdapter(Context context, List<TrojanConfig> data) {
        super();
        this.mData = new ArrayList<>(data.size());
        this.mPingDelayTimeView = new ArrayList<>(data.size());
        for (TrojanConfig config : data) {
            mData.add(new TrojanConfigWrapper(config));
            mPingDelayTimeView.add((float) -1);
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
        mPingDelayTimeView.clear();
        for (TrojanConfig config : data) {
            mData.add(new TrojanConfigWrapper(config));
            mPingDelayTimeView.add(ServerListDataManager.SERVER_STATUS_INIT);
        }
        notifyDataSetChanged();
    }

    public void setPingServerDelayTime(TrojanConfig config, float timeout) {
        int index = 0;
        for (TrojanConfigWrapper configWrapper : mData) {
            if (configWrapper.getRemoteAddr().equals(config.getRemoteAddr())) {
                mPingDelayTimeView.set(index, timeout);
                notifyItemChanged(index);
                break;
            }
            index += 1;
        }
    }

    public void removeItemOnPosition(int pos) {
        mData.remove(pos);
        notifyItemRemoved(pos);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.bindData(mData.get(i), mBatchDeleteMode, mPingDelayTimeView.get(i));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    @Override
    public void onRowMove(int srcPosition, int destPosition) {
        Collections.swap(mData, srcPosition, destPosition);
        notifyItemMoved(srcPosition, destPosition);
    }

    @Override
    public void onRowSelected(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof ViewHolder) {
            ViewHolder vh = (ViewHolder) viewHolder;
            vh.setItemSelected(true);
        }
    }

    @Override
    public void onRowClear(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof ViewHolder) {
            ViewHolder vh = (ViewHolder) viewHolder;
            vh.setItemSelected(false);
        }
    }

    public interface OnItemClickListener {
        void onItemSelected(TrojanConfig config, int pos);

        void onItemBatchSelected(TrojanConfig config, int pos, boolean checked);
    }
}

class ViewHolder extends RecyclerView.ViewHolder {
    private TrojanConfigWrapper mConfig;
    private TextView mRemoteServerRemarkTv;
    private TextView mPingTimout;
    private CheckBox mCheckBox;
    private boolean mBatchDeleteMode;
    private ServerListAdapter.OnItemClickListener mItemClickListener;
    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = ((checkBox, isChecked) -> {
        if (mItemClickListener != null) {
            mConfig.setSelected(isChecked);
            mItemClickListener.onItemBatchSelected(mConfig, getBindingAdapterPosition(), isChecked);
        }
    });

    public ViewHolder(@NonNull final View itemView) {
        super(itemView);
        mRemoteServerRemarkTv = itemView.findViewById(R.id.serverRemarkTv);
        itemView.setOnClickListener(v -> {
            if (mItemClickListener != null) {
                if (mBatchDeleteMode) {
                    mCheckBox.setChecked(!mCheckBox.isChecked());
                } else {
                    mItemClickListener.onItemSelected(mConfig.getDelegate(), getBindingAdapterPosition());
                }
            }
        });
        mCheckBox = itemView.findViewById(R.id.serverCb);
        mPingTimout = itemView.findViewById(R.id.server_speed);
    }

    public void bindData(TrojanConfigWrapper config, boolean batchDeleteMode, Float pingTimout) {
        this.mConfig = config;
        String name = config.getRemoteServerRemark();
        if (TextUtils.isEmpty(name)) { // only display remote address when remark is empty.
            name = config.getRemoteAddr();
        }
        mRemoteServerRemarkTv.setText(name);

        if (batchDeleteMode == true) {

        }

//        mCheckBox.setVisibility(batchDeleteMode ? View.VISIBLE : View.GONE);
        if (batchDeleteMode) {
            mPingTimout.setVisibility(View.GONE);
            mCheckBox.setVisibility(View.VISIBLE);
        } else {
            mCheckBox.setVisibility(View.GONE);
        }
        mCheckBox.setOnCheckedChangeListener(null);
        mCheckBox.setChecked(config.isSelected());
        mCheckBox.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mBatchDeleteMode = batchDeleteMode;
        if (pingTimout == ServerListDataManager.SERVER_STATUS_INIT) {
            mPingTimout.setVisibility(View.INVISIBLE);
        } else if (pingTimout == ServerListDataManager.SERVER_UNABLE_TO_REACH) {
            mPingTimout.setText(mPingTimout.getContext().getString(R.string.trojan_service_not_available));
        } else{

            mPingTimout.setVisibility(View.VISIBLE);
            if (pingTimout > 1000) {
                BigDecimal b = new BigDecimal(pingTimout/1000);
                float pintTime = b.setScale(2,BigDecimal.ROUND_HALF_UP).floatValue();
                mPingTimout.setText(String.valueOf(pintTime) + " s");
            } else {
                mPingTimout.setText(String.valueOf(pingTimout) + " ms");
            }
        }
    }

    public void setItemSelected(boolean selected) {
        if (selected) {
            int curTexColor = mRemoteServerRemarkTv.getCurrentTextColor();
            mRemoteServerRemarkTv.setTag(curTexColor);
            mRemoteServerRemarkTv.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.item_selected_text_foreground));
        } else {
            Integer previousColor = (Integer) mRemoteServerRemarkTv.getTag();
            if (previousColor != null) {
                mRemoteServerRemarkTv.setTextColor(previousColor);
            }
        }
    }

    public void bindListener(ServerListAdapter.OnItemClickListener listener) {
        mItemClickListener = listener;
    }
}
