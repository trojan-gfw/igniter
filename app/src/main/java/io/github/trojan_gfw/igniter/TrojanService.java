package io.github.trojan_gfw.igniter;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

public class TrojanService extends VpnService {
    private ParcelFileDescriptor pfd;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        VpnService.Builder b = new VpnService.Builder();
        try {
            b.addDisallowedApplication(getPackageName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        b.setSession(getString(R.string.app_name));
        b.setMtu(1500);
        b.addAddress("10.114.51.4", 31);
        b.addAddress("fd00:114:514::", 127);
        b.addRoute("0.0.0.0", 0);
        b.addRoute("::", 0);
        b.addDnsServer("1.0.0.1");
        b.addDnsServer("8.8.4.4");
        b.addDnsServer("1.1.1.1");
        b.addDnsServer("8.8.8.8");
        pfd = b.establish();
        int fd = pfd.getFd();
        String path = getCacheDir() + "/config.json";
        // TODO: Launch trojan and tun2socks, and send fd to tun2socks.
        return START_STICKY;
    }
}
