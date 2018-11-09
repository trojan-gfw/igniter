package io.github.trojan_gfw.igniter;

import android.content.Intent;
import android.net.VpnService;

public class TrojanService extends VpnService {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        VpnService.Builder b = new VpnService.Builder();
        try {
            b.addDisallowedApplication("io.github.trojan_gfw.igniter");
        } catch (Exception e) {
            e.printStackTrace();
        }
        b.addAddress("10.114.51.4", 31);
        b.establish();
        return START_STICKY;
    }
}
