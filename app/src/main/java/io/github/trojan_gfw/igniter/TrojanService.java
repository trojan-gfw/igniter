package io.github.trojan_gfw.igniter;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;

import clashm.Clashm;


public class TrojanService extends VpnService {
    private static final int VPN_MTU = 1500;
    private static final String PRIVATE_VLAN4_CLIENT = "172.19.0.1";
    private static final String PRIVATE_VLAN4_ROUTER = "172.19.0.2";
    private static final String PRIVATE_VLAN6_CLIENT = "fdfe:dcba:9876::1";
    private static final String PRIVATE_VLAN6_ROUTER = "fdfe:dcba:9876::2";
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
        boolean enable_clash = intent.getBooleanExtra("enable_clash", true);
        boolean enable_ipv6 = false;

        File file = new File(getFilesDir(), "config.json");
        if (file.exists()) {
            try {
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] content = new byte[(int) file.length()];
                    fis.read(content);
                    JSONObject json = new JSONObject(new String(content));
                    enable_ipv6 = json.getBoolean("enable_ipv6");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        b.setSession(getString(R.string.app_name));
        b.setMtu(VPN_MTU);
        b.addAddress(PRIVATE_VLAN4_CLIENT, 30);
        b.addRoute("0.0.0.0", 0);
        if (enable_ipv6) {
            b.addAddress(PRIVATE_VLAN6_CLIENT, 126);
            b.addRoute("::", 0);
        }
        b.addDnsServer("8.8.8.8");
        b.addDnsServer("8.8.4.4");
        b.addDnsServer("1.1.1.1");
        b.addDnsServer("1.0.0.1");
        if (enable_ipv6) {
            b.addDnsServer("2001:4860:4860::8888");
            b.addDnsServer("2001:4860:4860::8844");
        }
        pfd = b.establish();
        JNIHelper.trojan(getFilesDir() + "/config.json");

        int n2s_port;
        if (enable_clash) {
            try {
                Clashm.start(getFilesDir().toString());
                Log.e("Clash", "clash started");
            } catch (Exception e) {
                e.printStackTrace();
            }
            n2s_port = 1080;
        } else {
            n2s_port = 1081;

        }
        JNIHelper.n2s(pfd.getFd(), PRIVATE_VLAN4_ROUTER, "255.255.255.253",
                PRIVATE_VLAN6_ROUTER, VPN_MTU, "127.0.0.1", n2s_port);

        return START_STICKY;
    }

    private void shutdown() {
        try {
            if (Clashm.isRunning()) {
                Clashm.stop();
                Log.e("Clash", "clash stopped");
            }
            JNIHelper.stop();
            pfd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        shutdown();
        stopSelf();
    }
}
