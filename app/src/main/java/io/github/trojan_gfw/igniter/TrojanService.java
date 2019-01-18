package io.github.trojan_gfw.igniter;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

public class TrojanService extends VpnService {
    private static TrojanService instance;
    private ParcelFileDescriptor pfd;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    public static TrojanService getInstance() {
        return instance;
    }

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
        pfd = b.establish();
        JNIHelper.trojan(getFilesDir() + "/config.json");
        JNIHelper.n2s(pfd.getFd(), "10.114.51.5", "255.255.255.254", "", 1500, "127.0.0.1", 1080);
        return START_STICKY;
    }

    public void stop() {
        try {
            JNIHelper.stop();
            pfd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        stopSelf();
    }
}
