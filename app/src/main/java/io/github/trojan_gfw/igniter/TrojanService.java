package io.github.trojan_gfw.igniter;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import clash.Clash;
import tun2socks.Tun2socks;
import tun2socks.PacketFlow;


class Flow implements PacketFlow {
    private FileOutputStream flowOutputStream;

    Flow(FileOutputStream stream){
        flowOutputStream = stream;
    }

    @Override
    public void writePacket(byte[] bytes) {
        try{
            flowOutputStream.write(bytes);
        } catch (java.io.IOException e){
            e.printStackTrace();
        }
    }
}


public class TrojanService extends VpnService {
    private static final int VPN_MTU = 1500;
    private static final String PRIVATE_VLAN4_CLIENT = "172.19.0.1";
    private static final String PRIVATE_VLAN4_ROUTER = "172.19.0.2";
    private static final String PRIVATE_VLAN6_CLIENT = "fdfe:dcba:9876::1";
    private static final String PRIVATE_VLAN6_ROUTER = "fdfe:dcba:9876::2";
    private static TrojanService instance;
    private ParcelFileDescriptor pfd;
    private InputStream inputStream;
    private FileOutputStream outputStream;
    private ByteBuffer buffer = ByteBuffer.allocate(16 * 1024);
    private boolean running = false;
    private Thread packetThread;


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

    class PacketThread extends Thread {
        public void run()
        {
            System.out.println("Current thread name: "
                    + Thread.currentThread().getName());
            while (running) {
                try {
                    int n = inputStream.read(buffer.array());
                    if (n > 0) {
                        buffer.limit(n);
                        Tun2socks.inputPacket(buffer.array());
                        buffer.clear();
                    }
                } catch (IOException e) {
                    break;
                }
            }
            System.out.println("thread quit");
            return;
        }
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

        if ((pfd == null) || !Tun2socks.setNonblock((long) pfd.getFd(), false)) {
            Log.e("tun2socks", "failed to put tunFd in blocking mode");
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
        running = true;
        packetThread = new PacketThread();
        packetThread.start();

        return START_STICKY;
    }

    private void shutdown() {
        running = false;
        try {
            if (Clash.isRunning()) {
                Clash.stop();
                Log.e("Clash", "clash stopped");
            }
            Tun2socks.stop();
            JNIHelper.stop();
            pfd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        pfd = null;
        inputStream = null;
        outputStream = null;
    }

    public void stop() {
        shutdown();
        stopSelf();
    }
}
