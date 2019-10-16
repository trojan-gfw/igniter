package io.github.trojan_gfw.igniter;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import clash.Clash;
import tun2socks.PacketFlow;
import tun2socks.Tun2socks;


class Flow implements PacketFlow {
    private FileOutputStream flowOutputStream;

    Flow(FileOutputStream stream) {
        flowOutputStream = stream;
    }

    @Override
    public void writePacket(byte[] bytes) {
        try {
            if (flowOutputStream.getFD().valid())
                flowOutputStream.write(bytes);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}


public class ProxyService extends VpnService {
    public static final int STARTING = 0;
    public static final int STARTED = 1;
    public static final int STOPPING = 2;
    public static final int STOPPED = 3;
    public static final String STATUS_EXTRA_NAME = "service_state";
    public static final String CLASH_EXTRA_NAME = "enable_clash";

    private static final int VPN_MTU = 1500;
    private static final String PRIVATE_VLAN4_CLIENT = "172.19.0.1";
    //private static final String PRIVATE_VLAN4_ROUTER = "172.19.0.2";
    private static final String PRIVATE_VLAN6_CLIENT = "fdfe:dcba:9876::1";
    //private static final String PRIVATE_VLAN6_ROUTER = "fdfe:dcba:9876::2";
    private static ProxyService instance;
    private int state = STARTED;
    private ParcelFileDescriptor pfd;
    private InputStream inputStream;
    private FileOutputStream outputStream;
    private ByteBuffer packetBuffer = ByteBuffer.allocate(16 * 1024);
    private LocalBroadcastManager broadcastManager;

    public static ProxyService getInstance() {
        return instance;
    }

    private void setState(int state) {
        this.state = state;
        sendStateChangeBroadcast();
    }

    public int getState(){
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
        inputStream = null;
        outputStream = null;
    }

    private void sendStateChangeBroadcast() {
        Intent intent = new Intent(getString(R.string.bc_service_state));
        intent.putExtra(STATUS_EXTRA_NAME, state);
        broadcastManager.sendBroadcast(intent);
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
        boolean enable_clash = intent.getBooleanExtra(CLASH_EXTRA_NAME, true);
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
        b.setBlocking(true);
        pfd = b.establish();
        Log.e("VPN", "pfd established");

        if (pfd == null) {
            shutdown();
            return START_NOT_STICKY;
        }
        outputStream = new FileOutputStream(pfd.getFileDescriptor());
        inputStream = new FileInputStream(pfd.getFileDescriptor());
        Flow flow = new Flow(outputStream);

        JNIHelper.trojan(getFilesDir() + "/config.json");

        int tun2socksPort;
        if (enable_clash) {
            try {
                Clash.start(getFilesDir().toString());
                Log.e("Clash", "clash started");
            } catch (Exception e) {
                e.printStackTrace();
            }
            tun2socksPort = 1080;
        } else {
            tun2socksPort = 1081;
        }
        Tun2socks.start(flow, "127.0.0.1:" + tun2socksPort, "255.0.128.1", "255.0.143.254");
        new PacketThread().start();

        setState(STARTED);

        return START_STICKY;
    }

    private void shutdown() {
        setState(STOPPING);

        JNIHelper.stop();
        if (Clash.isRunning()) {
            Clash.stop();
            Log.e("Clash", "clash stopped");
        }
        Tun2socks.stop();
    }

    public void stop() {
        shutdown();
    }

    class PacketThread extends Thread {
        private static final String TAG = "TunPacketThread";

        public void run() {
            Log.e(TAG, Thread.currentThread().getName() + " thread start");
            while (state == STARTING || state == STARTED) {
                try {
                    int n = inputStream.read(packetBuffer.array());
                    if (n > 0) {
                        packetBuffer.limit(n);
                        Tun2socks.inputPacket(packetBuffer.array());
                        packetBuffer.clear();
                    }
                } catch (IOException e) {
                    break;
                }
            }
            Log.e(TAG, Thread.currentThread().getName() + " thread exit");
            try {
                pfd.close();
                Log.e("VPN", "pfd closed");
            } catch (Exception e) {
                e.printStackTrace();
            }
            stopSelf();
        }
    }
}
