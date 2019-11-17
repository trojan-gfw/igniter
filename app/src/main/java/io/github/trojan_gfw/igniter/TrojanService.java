package io.github.trojan_gfw.igniter;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

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
        shutdown();
        instance = null;
    }

    public static TrojanService getInstance() {
        return instance;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startService();

        return START_STICKY;
    }

    public void startService() {
        VpnService.Builder b = new VpnService.Builder();

        configVpnBuilder(b);

        pfd = b.establish();
        JNIHelper.trojan(String.format("%s/%s", getFilesDir(), AppConfig.SERVER_CONFIG_FILE));
        JNIHelper.n2s(pfd.getFd(),
                PRIVATE_VLAN4_ROUTER,
                "255.255.255.253",
                PRIVATE_VLAN6_ROUTER,
                VPN_MTU,
                "127.0.0.1",
                1080);
    }

    private void configVpnBuilder(VpnService.Builder b) {
        AppConfig appConfig = AppConfig.getInstance();
        Boolean enableIPv6 = appConfig.getEnableIPv6();

        try {
            b.addDisallowedApplication(getPackageName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        b.setSession(getString(R.string.app_name));
        b.setMtu(VPN_MTU);
        b.addAddress(PRIVATE_VLAN4_CLIENT, 30);
        b.addRoute("0.0.0.0", 0);

        if (enableIPv6) {
            b.addAddress(PRIVATE_VLAN6_CLIENT, 126);
            b.addRoute("::", 0);
        }

        b.addDnsServer("8.8.8.8");
        b.addDnsServer("8.8.4.4");
        b.addDnsServer("1.1.1.1");
        b.addDnsServer("1.0.0.1");
        if (enableIPv6) {
            b.addDnsServer("2001:4860:4860::8888");
            b.addDnsServer("2001:4860:4860::8844");
        }
    }

    public void stopService() {
        shutdown();
        stopSelf();
    }

    private void shutdown() {
        try {
            JNIHelper.stop();
            if (pfd != null) pfd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
