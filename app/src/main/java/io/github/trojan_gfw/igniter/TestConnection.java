package io.github.trojan_gfw.igniter;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;


class TestConnectionResult {
    final String url;
    final boolean isConnected;
    final Exception error;
    final long time; // In milliseconds

    TestConnectionResult(String url, boolean isConnected, Exception error, long time) {
        this.url = url;
        this.isConnected = isConnected;
        this.error = error;
        this.time = time;
    }
}


class TestConnection extends AsyncTask<String, Void, TestConnectionResult> {
    private static final int DEFAULT_TIMEOUT = 10 * 1000; // 10 seconds

    private WeakReference<MainActivity> activityReference;

    TestConnection(MainActivity context) {
        activityReference = new WeakReference<>(context);
    }

    protected TestConnectionResult doInBackground(String... urls) {
        String url = urls[0];
        try {
            long startTime = System.currentTimeMillis();
            ProxyService serviceInstance = ProxyService.getInstance();
            InetSocketAddress proxy_address = new InetSocketAddress("127.0.0.1",
                    (int)serviceInstance.tun2socksPort);
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, proxy_address);
            URLConnection connection = new URL(url).openConnection(proxy);
            connection.setConnectTimeout(DEFAULT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_TIMEOUT);
            connection.connect();
            return new TestConnectionResult(url, true, null,
                    System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            return new TestConnectionResult(url, false, e, 0);
        }
    }

    protected void onPostExecute(TestConnectionResult result) {
        MainActivity activity = activityReference.get();
        if (activity != null) {
            if (result.isConnected) {
                Toast.makeText(activity,
                        activity.getString(R.string.connected_to__in__ms,
                                result.url, String.valueOf(result.time)),
                        Toast.LENGTH_LONG).show();
            } else {
                result.error.printStackTrace();
                Toast.makeText(activity,
                        activity.getString(R.string.failed_to_connect_to__,
                                result.url, "Please start igniter before testing"),
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
