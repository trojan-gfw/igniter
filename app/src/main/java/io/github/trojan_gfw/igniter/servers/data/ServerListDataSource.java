package io.github.trojan_gfw.igniter.servers.data;

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

    interface Callback {
        void onSuccess();
        void onFailed();
    }
}
