package io.github.trojan_gfw.igniter.connection;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

public class TestConnection {
    private static final int DEFAULT_TIMEOUT = 10 * 1000; // 10 seconds
    private final String mProxyHost;
    private final long mProxyPort;
    private final WeakReference<OnResultListener> mOnResultListenerRef;

    public TestConnection(String proxyHost, long proxyPort, OnResultListener onResultListener) {
        mProxyHost = proxyHost;
        mProxyPort = proxyPort;
        mOnResultListenerRef = new WeakReference<>(onResultListener);
    }

    public void testLatency(String testUrl) {
        try {
            long startTime = System.currentTimeMillis();
            InetSocketAddress proxyAddress = new InetSocketAddress(mProxyHost, (int) mProxyPort);
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, proxyAddress);
            URLConnection connection = new URL(testUrl).openConnection(proxy);
            connection.setConnectTimeout(DEFAULT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_TIMEOUT);
            connection.connect();
            postResult(testUrl, true, "", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            postResult(testUrl, false, e.getMessage(), 0L);
        }
    }

    private void postResult(String url, boolean connected, String error, long latency) {
        OnResultListener listener = mOnResultListenerRef.get();
        if (listener != null) {
            listener.onResult(url, connected, latency, error);
        }
    }

    public interface OnResultListener {
        void onResult(String testUrl, boolean connected, long delay, String error);
    }
}
