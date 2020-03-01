package io.github.trojan_gfw.igniter.servers.data;

import java.util.List;

import io.github.trojan_gfw.igniter.TrojanConfig;

public interface ServerListDataSource {
    List<TrojanConfig> loadServerConfigList();
    void deleteServerConfig(TrojanConfig config);
    void saveServerConfig(TrojanConfig config);
    void replaceServerConfigs(List<TrojanConfig> list);
}
