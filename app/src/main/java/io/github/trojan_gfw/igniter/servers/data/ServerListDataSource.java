package io.github.trojan_gfw.igniter.servers.data;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.Collection;
import java.util.List;

import io.github.trojan_gfw.igniter.TrojanConfig;

public interface ServerListDataSource {
    @WorkerThread
    List<TrojanConfig> loadServerConfigList();
    @WorkerThread
    void deleteServerConfig(TrojanConfig config);
    @WorkerThread
    void batchDeleteServerConfigs(Collection<TrojanConfig> configs);
    @WorkerThread
    void saveServerConfig(TrojanConfig config);
    @WorkerThread
    void replaceServerConfigs(List<TrojanConfig> list);
    @WorkerThread
    void requestSubscribeServerConfigs(String url, @NonNull Callback callback);

    /**
     * Parse trojan configs from {@param fileUri}. Combine with current trojan config list and return
     * the complete list.
     *
     * @param context Context
     * @param fileUri File Uri
     * @return List of all trojan configs.
     */
    @WorkerThread
    List<TrojanConfig> importServersFromFile(Context context, Uri fileUri);
    @WorkerThread
    boolean exportServers(String exportPath);

    interface Callback {
        void onSuccess();
        void onFailed();
    }
}
