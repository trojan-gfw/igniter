package io.github.trojan_gfw.igniter;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;

import clash.Clash;
import freeport.Freeport;
import tun2socks.Tun2socks;
import tun2socks.Tun2socksStartOptions;


public class ProxyService extends VpnService {
    public static final int STARTING = 0;
    public static final int STARTED = 1;
    public static final int STOPPING = 2;
    public static final int STOPPED = 3;
    public static final String STATUS_EXTRA_NAME = "service_state";
    public static final String CLASH_EXTRA_NAME = "enable_clash";
    public static final int IGNITER_STATUS_NOTIFY_MSG_ID = 0;
    public long tun2socksPort;
    public boolean enable_clash = false;

    public static ProxyService getInstance() {
        return instance;
    }

    private static final int VPN_MTU = 1500;
    private static final String PRIVATE_VLAN4_CLIENT = "172.19.0.1";
    //private static final String PRIVATE_VLAN4_ROUTER = "172.19.0.2";
    private static final String PRIVATE_VLAN6_CLIENT = "fdfe:dcba:9876::1";
    //private static final String PRIVATE_VLAN6_ROUTER = "fdfe:dcba:9876::2";
    private static ProxyService instance;
    private int state = STARTED;
    private ParcelFileDescriptor pfd;
    private LocalBroadcastManager broadcastManager;

    private void setState(int state) {
        this.state = state;
        sendStateChangeBroadcast();
    }

    public int getState() {
        return state;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        broadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setState(STOPPED);
        instance = null;
        broadcastManager = null;
        pfd = null;
    }

    private void sendStateChangeBroadcast() {
        Intent intent = new Intent(getString(R.string.bc_service_state));
        intent.putExtra(STATUS_EXTRA_NAME, state);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onRevoke() {
        // Calls to this method may not happen on the main thread
        // of the process.
        stop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setState(STARTING);

        VpnService.Builder b = new VpnService.Builder();
        try {
            b.addDisallowedApplication(getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            setState(STOPPED);
        }
        enable_clash = intent.getBooleanExtra(CLASH_EXTRA_NAME, true);
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
        if (enable_clash) {
            for (String route : getResources().getStringArray(R.array.bypass_private_route)) {
                String[] parts = route.split("/", 2);
                b.addRoute(parts[0], Integer.parseInt(parts[1]));
            }
            // fake ip range for clash
            b.addRoute("255.0.128.0", 20);
        } else {
            b.addRoute("0.0.0.0", 0);
        }

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
        LogHelper.e("VPN", "pfd established");

        if (pfd == null) {
            shutdown();
            return START_NOT_STICKY;
        }
        int fd = pfd.detachFd();
        long trojanPort;
        try {
            trojanPort = Freeport.getFreePort();
        } catch (Exception e) {
            e.printStackTrace();
            trojanPort = 1081;
        }
        LogHelper.i("igniter", "trojan port is " + trojanPort);
        TrojanHelper.ChangeListenPort(Globals.getTrojanConfigPath(), trojanPort);
        TrojanHelper.ShowConfig(Globals.getTrojanConfigPath());

        JNIHelper.trojan(Globals.getTrojanConfigPath());

        long clashSocksPort = 1080; // default value in case fail to get free port
        if (enable_clash) {
            try {

                // clash and trojan should NOT listen on the same port
                do {
                    clashSocksPort = Freeport.getFreePort();
                }
                while (clashSocksPort == trojanPort);

                LogHelper.i("igniter", "clash port is " + clashSocksPort);
                ClashHelper.ChangeClashConfig(Globals.getClashConfigPath(),
                        trojanPort, clashSocksPort);
                ClashHelper.ShowConfig(Globals.getClashConfigPath());
                Clash.start(getFilesDir().toString());
                LogHelper.e("Clash", "clash started");
            } catch (Exception e) {
                e.printStackTrace();
            }
            tun2socksPort = clashSocksPort;
        } else {
            tun2socksPort = trojanPort;
        }
        LogHelper.i("igniter", "tun2socks port is " + tun2socksPort);

        // debug/info/warn/error/none
        Tun2socksStartOptions tun2socksStartOptions = new Tun2socksStartOptions();
        tun2socksStartOptions.setTunFd(fd);
        tun2socksStartOptions.setSocks5Server("127.0.0.1:" + tun2socksPort);
        tun2socksStartOptions.setEnableIPv6(enable_ipv6);
        tun2socksStartOptions.setMTU(VPN_MTU);

        Tun2socks.setLoglevel("info");
        if (enable_clash) {
            tun2socksStartOptions.setFakeIPStart("255.0.128.1");
            tun2socksStartOptions.setFakeIPStop("255.0.143.254");
        } else {
            // Disable go-tun2socks fake ip
            tun2socksStartOptions.setFakeIPStart("");
            tun2socksStartOptions.setFakeIPStop("");
        }
        Tun2socks.start(tun2socksStartOptions);
        LogHelper.i("igniter", tun2socksStartOptions.toString());

        StringBuilder runningStatusStringBuilder = new StringBuilder();
        runningStatusStringBuilder.append("Trojan SOCKS5 port: ")
                .append(trojanPort)
                .append("\n")
                .append("Tun2socks port: ")
                .append(tun2socksPort)
                .append("\n");
        if (enable_clash) {
            runningStatusStringBuilder.append("Clash SOCKS listen port: ")
                    .append(clashSocksPort)
                    .append("\n");
        }

        setState(STARTED);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        Intent openMainActivityIntent = new Intent(this, MainActivity.class);
        openMainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingOpenMainActivityIntent = PendingIntent.getActivity(this, 0, openMainActivityIntent, 0);
        String igniterRunningStatusStr = runningStatusStringBuilder.toString();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Igniter is running")
                .setContentText(igniterRunningStatusStr)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(igniterRunningStatusStr))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingOpenMainActivityIntent)
                .setAutoCancel(false)
                .setOngoing(true);
        notificationManager.notify(IGNITER_STATUS_NOTIFY_MSG_ID, builder.build());

        return START_STICKY;
    }

    private void shutdown() {
        setState(STOPPING);

        JNIHelper.stop();
        if (Clash.isRunning()) {
            Clash.stop();
            LogHelper.e("Clash", "clash stopped");
        }
        Tun2socks.stop();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(IGNITER_STATUS_NOTIFY_MSG_ID);

        stopSelf();

        setState(STOPPED);
        instance = null;
    }

    public void stop() {
        shutdown();
    }
}
