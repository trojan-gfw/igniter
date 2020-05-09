package io.github.trojan_gfw.igniter.servers.data;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.github.trojan_gfw.igniter.TrojanConfig;
import io.github.trojan_gfw.igniter.TrojanHelper;

public class ServerListDataManager implements ServerListDataSource {
    private final String mConfigFilePath;

    public ServerListDataManager(String configFilePath) {
        mConfigFilePath = configFilePath;
    }

    @Override
    @NonNull
    public List<TrojanConfig> loadServerConfigList() {
        return new ArrayList<>(TrojanHelper.readTrojanServerConfigList(mConfigFilePath));
    }

    @Override
    public void deleteServerConfig(TrojanConfig config) {
        List<TrojanConfig> trojanConfigs = loadServerConfigList();
        for (int i = trojanConfigs.size() - 1; i >= 0; i--) {
            if (trojanConfigs.get(i).getRemoteAddr().equals(config.getRemoteAddr())) {
                trojanConfigs.remove(i);
                replaceServerConfigs(trojanConfigs);
                break;
            }
        }
    }

    @Override
    public void saveServerConfig(TrojanConfig config) {
        if (config == null) {
            return;
        }
        final String remoteAddr = config.getRemoteAddr();
        if (remoteAddr == null) {
            return;
        }
        boolean configRemoteAddrExists = false;
        List<TrojanConfig> trojanConfigs = loadServerConfigList();
        for (int i = trojanConfigs.size() - 1; i >= 0; i--) {
            TrojanConfig cacheConfig = trojanConfigs.get(i);
            if (cacheConfig == null) continue;
            if (remoteAddr.equals(cacheConfig.getRemoteAddr())) {
                trojanConfigs.set(i, config);
                configRemoteAddrExists = true;
                break;
            }
        }
        if (!configRemoteAddrExists) {
            trojanConfigs.add(config);
        }
        replaceServerConfigs(trojanConfigs);
    }

    @Override
    public void replaceServerConfigs(List<TrojanConfig> list) {
        TrojanHelper.writeTrojanServerConfigList(list, mConfigFilePath);
        TrojanHelper.ShowTrojanConfigList(mConfigFilePath);
    }
}
