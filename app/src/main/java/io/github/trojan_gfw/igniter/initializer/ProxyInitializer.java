package io.github.trojan_gfw.igniter.initializer;

import android.content.Context;

import io.github.trojan_gfw.igniter.Globals;
import io.github.trojan_gfw.igniter.LogHelper;
import io.github.trojan_gfw.igniter.TrojanConfig;
import io.github.trojan_gfw.igniter.TrojanHelper;

public class ProxyInitializer extends Initializer {
    private static final String TAG = "ProxyInitializer";

    @Override
    public void init(Context context) {
        Globals.Init(context);
        TrojanConfig cacheConfig = TrojanHelper.readTrojanConfig(Globals.getTrojanConfigPath());
        if (cacheConfig == null) {
            LogHelper.e(TAG, "read null trojan config");
        } else {
            cacheConfig.setCaCertPath(Globals.getCaCertPath());
            Globals.setTrojanConfigInstance(cacheConfig);
        }
        if (!Globals.getTrojanConfigInstance().isValidRunningConfig()) {
            LogHelper.e(TAG, "Invalid trojan config!");
        }
    }

    @Override
    public boolean runsInWorkerThread() {
        return false;
    }
}
