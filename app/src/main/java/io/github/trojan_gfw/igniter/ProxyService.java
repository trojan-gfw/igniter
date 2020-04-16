package io.github.trojan_gfw.igniter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import androidx.annotation.IntDef;
import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.Set;

import clash.Clash;
import freeport.Freeport;
import io.github.trojan_gfw.igniter.common.utils.PermissionUtils;
import io.github.trojan_gfw.igniter.connection.TestConnection;
import io.github.trojan_gfw.igniter.exempt.data.ExemptAppDataManager;
import io.github.trojan_gfw.igniter.exempt.data.ExemptAppDataSource;
import io.github.trojan_gfw.igniter.proxy.aidl.ITrojanService;
import io.github.trojan_gfw.igniter.proxy.aidl.ITrojanServiceCallback;
import tun2socks.Tun2socks;
import tun2socks.Tun2socksStartOptions;

/**
 * A service that provides proxy connection management, including starting or stopping proxy connection,
 * test connection and state change. You should call {@link #startForegroundService(Intent)} to start
 * this service and send broadcast with action {@link R.string#stop_service} to shutdown the service.
 * It's recommended to start this service by the help
 * of {@link io.github.trojan_gfw.igniter.tile.ProxyHelper}.
 * <br/>
 * If you want to interact withthe service, you should call {@link #bindService(Intent, ServiceConnection, int)}
 * with the action {@link R.string#bind_service}. Then {@link ProxyService} will return a binder
 * which implements {@link ITrojanService} at {@link #onBind(Intent)}.
 * <br/>
 * If you want to listen for state change and test connection result, you have to implement
 * {@link ITrojanServiceCallback} and register it once {@link ServiceConnection#onServiceConnected(ComponentName, IBinder)}
 * is triggered. Don't forget to unregister the callback when binder is died or service disconnected.
 * <br/>
 * Since the interaction is quite complex, it's recommended to interact with {@link ProxyService} by
 * {@link io.github.trojan_gfw.igniter.connection.TrojanConnection}. For further information, see
 * {@link io.github.trojan_gfw.igniter.connection.TrojanConnection}.
 */
public class ProxyService extends VpnService implements TestConnection.OnResultListener {
    private static final String TAG = "ProxyService";
    public static final int STATE_NONE = -1;
    public static final int STARTING = 0;
    public static final int STARTED = 1;
    public static final int STOPPING = 2;
    public static final int STOPPED = 3;
    public static final String CLASH_EXTRA_NAME = "enable_clash";
    public static final int IGNITER_STATUS_NOTIFY_MSG_ID = 114514;
    public long tun2socksPort;
    public boolean enable_clash = false;

    @IntDef({STATE_NONE, STARTING, STARTED, STOPPING, STOPPED})
    public @interface ProxyState {
    }

    private static final int VPN_MTU = 1500;
    private static final String PRIVATE_VLAN4_CLIENT = "172.19.0.1";
    //private static final String PRIVATE_VLAN4_ROUTER = "172.19.0.2";
    private static final String PRIVATE_VLAN6_CLIENT = "fdfe:dcba:9876::1";
    //private static final String PRIVATE_VLAN6_ROUTER = "fdfe:dcba:9876::2";
    private static final String TUN2SOCKS5_SERVER_HOST = "127.0.0.1";
    private @ProxyState
    int state = STATE_NONE;
    private ParcelFileDescriptor pfd;
    private ExemptAppDataSource mExemptAppDataSource;
    /**
     * Receives stop event.
     */
    private BroadcastReceiver mStopBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String stopAction = getString(R.string.stop_service);
            final String action = intent.getAction();
            if (stopAction.equals(action)) {
                stop();
            }
        }
    };
    /**
     * Callback list for remote processes or services.
     */
    private final RemoteCallbackList<ITrojanServiceCallback> mCallbackList = new RemoteCallbackList<>();
    /**
     * Binder implementation of {@link ITrojanService}, which provides access of connection state,
     * connection test and callback registration.
     */
    private final IBinder mBinder = new ITrojanService.Stub() {
        @Override
        public int getState() {
            LogHelper.i(TAG, "IBinder getState# : " + state);
            return state;
        }

        @Override
        public void testConnection(String testUrl) {
            if (state != STARTED) {
                onResult(TUN2SOCKS5_SERVER_HOST, false, 0L, "ProxyService not yet connected.");
                return;
            }
            new TestConnection(TUN2SOCKS5_SERVER_HOST, tun2socksPort, ProxyService.this).execute(testUrl);
        }

        @Override
        public void showDevelopInfoInLogcat() {
            LogHelper.showDevelopInfoInLogcat();
        }

        @Override
        public void registerCallback(ITrojanServiceCallback callback) {
            LogHelper.i(TAG, "IBinder registerCallback#");
            mCallbackList.register(callback);
        }

        @Override
        public void unregisterCallback(ITrojanServiceCallback callback) {
            LogHelper.i(TAG, "IBinder unregisterCallback#");
            mCallbackList.unregister(callback);
        }
    };

    private void setState(int state) {
        LogHelper.i(TAG, "setState: " + state);
        this.state = state;
        notifyStateChange();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogHelper.i(TAG, "onCreate");
        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.stop_service));
        registerReceiver(mStopBroadcastReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogHelper.i(TAG, "onDestroy");
        mCallbackList.kill();
        setState(STOPPED);
        unregisterReceiver(mStopBroadcastReceiver);
        pfd = null;
    }

    /**
     * Broadcast the state change event by invoking callbacks from other processes or services.
     */
    private void notifyStateChange() {
        int state = this.state;
        for (int i = mCallbackList.beginBroadcast() - 1; i >= 0; i--) {
            try {
                // the second String parameter is currently useless. Might be the url of the profile.
                mCallbackList.getBroadcastItem(i).onStateChanged(state, "state changed");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mCallbackList.finishBroadcast();
    }

    @Override
    public void onRevoke() {
        // Calls to this method may not happen on the main thread
        // of the process.
        stop();
    }

    @Override
    public void onResult(String testUrl, boolean connected, long delay, String error) {
        // broadcast test result by invoking callbacks from other processes or services.
        for (int i = mCallbackList.beginBroadcast() - 1; i >= 0; i--) {
            try {
                mCallbackList.getBroadcastItem(i).onTestResult(testUrl, connected, delay, error);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mCallbackList.finishBroadcast();
    }

    @Override
    public IBinder onBind(Intent intent) {
        final String bindServiceAction = getString(R.string.bind_service);
        if (bindServiceAction.equals(intent.getAction())) {
            return mBinder;
        }
        return super.onBind(intent);
    }

    private Set<String> getExemptAppPackageNames() {
        if (!PermissionUtils.hasReadWriteExtStoragePermission(this)) {
            return Collections.emptySet();
        }
        if (mExemptAppDataSource == null) {
            mExemptAppDataSource = new ExemptAppDataManager(getApplicationContext(), Globals.getExemptedAppListPath());
        }
        // ensures that new exempted app list can be applied on proxy after modification.
        return mExemptAppDataSource.loadExemptAppPackageNameSet();
    }

    /**
     * Start foreground notification to avoid ANR and crash, as Android requires that Service which
     * is started by calling {@link Context#startForegroundService(Intent)} must
     * invoke {@link android.app.Service#startForeground(int, Notification)} within 5 seconds.
     */
    private void startForegroundNotification(String channelId) {
        Intent openMainActivityIntent = new Intent(this, MainActivity.class);
        openMainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingOpenMainActivityIntent = PendingIntent.getActivity(this, 0, openMainActivityIntent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_tile)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_starting_service))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingOpenMainActivityIntent)
                .setAutoCancel(false)
                .setOngoing(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setShowWhen(true);
        }
        builder.setWhen(0L);

        // it's required to create a notification channel before startForeground on SDK >= Android O
        createNotificationChannel(channelId);
        LogHelper.i(TAG, "start foreground notification");
        startForeground(IGNITER_STATUS_NOTIFY_MSG_ID, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogHelper.i(TAG, "onStartCommand");
        // In order to keep the service long-lived, starting the service by Context.startForegroundService()
        // might be the easiest way. According to the official indication, a service which is started
        // by Context.startForegroundService() must call Service.startForeground() within 5 seconds.
        // Otherwise the process will be shutdown and user will get an ANR notification.
        startForegroundNotification(getString(R.string.notification_channel_id));
        setState(STARTING);

        Set<String> exemptAppPackageNames = getExemptAppPackageNames();

        VpnService.Builder b = new VpnService.Builder();
        try {
            b.addDisallowedApplication(getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            setState(STOPPED);
            // todo: stop foreground notification and return here?
        }
        for (String packageName : exemptAppPackageNames) {
            try {
                b.addDisallowedApplication(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
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
            // fake ip range for go-tun2socks
            // should match clash configuration
            b.addRoute("198.18.0.0", 16);
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
        LogHelper.i("VPN", "pfd established");

        if (pfd == null) {
            stop();
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
        LogHelper.i("Igniter", "trojan port is " + trojanPort);
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
                LogHelper.i("Clash", "clash started");
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
        tun2socksStartOptions.setSocks5Server(TUN2SOCKS5_SERVER_HOST + ":" + tun2socksPort);
        tun2socksStartOptions.setEnableIPv6(enable_ipv6);
        tun2socksStartOptions.setMTU(VPN_MTU);

        Tun2socks.setLoglevel("info");
        if (enable_clash) {
            tun2socksStartOptions.setFakeIPRange("198.18.0.1/16");
        } else {
            // Disable go-tun2socks fake ip
            tun2socksStartOptions.setFakeIPRange("");
        }
        Tun2socks.start(tun2socksStartOptions);
        LogHelper.i(TAG, tun2socksStartOptions.toString());

        setState(STARTED);

        Intent openMainActivityIntent = new Intent(this, MainActivity.class);
        openMainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingOpenMainActivityIntent = PendingIntent.getActivity(this, 0, openMainActivityIntent, 0);
        String igniterRunningStatusStr = "listening on port: " + tun2socksPort;
        final String channelId = getString(R.string.notification_channel_id);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_tile)
                .setContentTitle("Igniter Active")
                .setContentText(igniterRunningStatusStr)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(igniterRunningStatusStr))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingOpenMainActivityIntent)
                .setAutoCancel(false)
                .setOngoing(true);
        startForeground(IGNITER_STATUS_NOTIFY_MSG_ID, builder.build());
        return START_STICKY;
    }

    private void shutdown() {
        LogHelper.i(TAG, "shutdown");
        setState(STOPPING);
        JNIHelper.stop();
        if (Clash.isRunning()) {
            Clash.stop();
            LogHelper.i("Clash", "clash stopped");
        }
        Tun2socks.stop();

        stopSelf();

        setState(STOPPED);
        stopForeground(true);
        destroyNotificationChannel(getString(R.string.notification_channel_id));
    }

    private void createNotificationChannel(String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(channelId,
                    getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void destroyNotificationChannel(String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.deleteNotificationChannel(channelId);
        }
    }

    public void stop() {
        shutdown();
        // this is essential for gomobile aar
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
