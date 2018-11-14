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
    private ParcelFileDescriptor pfd;
    private Process trojanProcess;
    private Process tun2socksProcess;

    private static void redirect(final InputStream is) {
        new Thread(new Runnable() {
            public void run() {
                Scanner sc = new Scanner(is);
                while (sc.hasNextLine()) {
                    Log.d("redirect", sc.nextLine());
                }
            }
        }).start();
    }

    private static Process exec(String command) throws IOException {
        Process p = Runtime.getRuntime().exec(command);
        redirect(p.getInputStream());
        redirect(p.getErrorStream());
        return p;
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
        int fd = pfd.getFd();
        String trojanConfigPath = getCacheDir() + "/config.json";
        String trojanExePath = getApplicationInfo().nativeLibraryDir + "/libtrojan.so";
        String tun2socksExePath = getApplicationInfo().nativeLibraryDir + "/libtun2socks.so";
        String sockPath = getCacheDir() + "/sock_path";
        try {
            new File(trojanExePath).setExecutable(true);
            new File(tun2socksExePath).setExecutable(true);
            trojanProcess = exec(trojanExePath + ' ' + trojanConfigPath);
            tun2socksProcess = exec(tun2socksExePath + " --netif-ipaddr 10.114.51.5 --netif-netmask 255.255.255.254 --socks-server-addr 127.0.0.1:1080 --tunfd " + String.valueOf(fd) + " --tunmtu 1500 --sock-path " + sockPath + " --loglevel debug --enable-udprelay --udprelay-max-connections 20");
            for (int i = 0; i < 5; ++i) {
                try {
                    JNIHelper.sendFd(fd, sockPath);
                    break;
                } catch (Exception e) {
                    Thread.sleep(1000);
                }
            }
            new File(sockPath).delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return START_STICKY;
    }
}
