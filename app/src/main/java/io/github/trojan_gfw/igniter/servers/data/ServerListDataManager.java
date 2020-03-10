package io.github.trojan_gfw.igniter.servers.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.github.trojan_gfw.igniter.TrojanConfig;
import io.github.trojan_gfw.igniter.TrojanHelper;

public class ServerListDataManager implements ServerListDataSource {
    private static final String SERVER_CONFIG_LIST_FILE_NAME = "config_list.txt";
    private final String mConfigFilePath;

    public ServerListDataManager(String configFileDir) {
        mConfigFilePath = configFileDir + File.separator + SERVER_CONFIG_LIST_FILE_NAME;
    }

    @Override
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
        boolean configRemoteAddrExists = false;
        List<TrojanConfig> trojanConfigs = loadServerConfigList();
        for (int i = trojanConfigs.size() - 1; i >= 0; i--) {
            if (trojanConfigs.get(i).getRemoteAddr().equals(config.getRemoteAddr())) {
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
    }
}
