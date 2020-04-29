package io.github.trojan_gfw.igniter.servers.fragment;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.TrojanConfig;

public class ServerListAdapter extends RecyclerView.Adapter<ViewHolder> {
    private final LayoutInflater mInflater;
    private final List<TrojanConfig> mData;
    private OnItemClickListener mOnItemClickListener;

    public ServerListAdapter(Context context, List<TrojanConfig> data) {
        super();
        this.mData = new ArrayList<>(data);
        mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        ViewHolder vh = new ViewHolder(mInflater.inflate(R.layout.item_server, viewGroup, false));
        vh.bindListener(mOnItemClickListener);
        return vh;
    }

    public void replaceData(List<TrojanConfig> data) {
        mData.clear();
        mData.addAll(data);
        notifyDataSetChanged();
    }

    public void removeItemOnPosition(int pos) {
        mData.remove(pos);
        notifyItemRemoved(pos);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.bindData(mData.get(i));
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

        void onItemDelete(TrojanConfig config, int pos);
    }
}

class ViewHolder extends RecyclerView.ViewHolder {
    private TrojanConfig mConfig;
    private TextView mRemoteAddrTv;
    private ServerListAdapter.OnItemClickListener itemClickListener;

    public ViewHolder(@NonNull final View itemView) {
        super(itemView);
        mRemoteAddrTv = itemView.findViewById(R.id.serverAddrTv);
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemClickListener != null) {
                    itemClickListener.onItemSelected(mConfig, getBindingAdapterPosition());
                }
            }
        });
        itemView.findViewById(R.id.deleteServerBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemClickListener != null) {
                    itemClickListener.onItemDelete(mConfig, getBindingAdapterPosition());
                }
            }
        });
    }

    public void bindData(TrojanConfig config) {
        this.mConfig = config;
        mRemoteAddrTv.setText(config.getRemoteAddr());
    }

    public void bindListener(ServerListAdapter.OnItemClickListener listener) {
        itemClickListener = listener;
    }
}
