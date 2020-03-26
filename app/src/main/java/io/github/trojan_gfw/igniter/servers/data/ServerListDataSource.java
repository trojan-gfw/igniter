package io.github.trojan_gfw.igniter.servers.data;

import android.support.annotation.WorkerThread;

import java.util.List;

import io.github.trojan_gfw.igniter.TrojanConfig;

public interface ServerListDataSource {
    @WorkerThread
    List<TrojanConfig> loadServerConfigList();
    @WorkerThread
    void deleteServerConfig(TrojanConfig config);
    @WorkerThread
    void saveServerConfig(TrojanConfig config);
    @WorkerThread
    void replaceServerConfigs(List<TrojanConfig> list);
}
