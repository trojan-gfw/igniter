package io.github.trojan_gfw.igniter;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

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
        int fd = pfd.detachFd();
        String trojanConfigPath = getCacheDir() + "/config.json";
        return START_STICKY;
    }
}
