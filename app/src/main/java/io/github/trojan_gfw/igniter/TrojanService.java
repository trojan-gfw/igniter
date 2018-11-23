package io.github.trojan_gfw.igniter;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

public class TrojanService extends VpnService {
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
        b.addRoute("0.0.0.0", 0);
        b.addDnsServer("1.0.0.1");
        b.addDnsServer("8.8.4.4");
        b.addDnsServer("1.1.1.1");
        b.addDnsServer("8.8.8.8");
        ParcelFileDescriptor pfd = b.establish();
        final int fd = pfd.detachFd();
        final String trojanConfigPath = getCacheDir() + "/config.json";
        new Thread(new Runnable() {
            @Override
            public void run() {
                JNIHelper.trojan(trojanConfigPath);
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                JNIHelper.n2s(fd, "10.114.51.5", "255.255.255.254", "", 1500, "127.0.0.1", 1080);
            }
        }).start();
        return START_STICKY;
    }
}
