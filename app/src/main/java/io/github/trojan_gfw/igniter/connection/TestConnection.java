package io.github.trojan_gfw.igniter.connection;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

public class TestConnection extends AsyncTask<String, Void, TestResult> {
    private static final int DEFAULT_TIMEOUT = 10 * 1000; // 10 seconds
    private final String mProxyHost;
    private final long mProxyPort;
    private final WeakReference<OnResultListener> mOnResultListenerRef;

    public TestConnection(String proxyHost, long proxyPort, OnResultListener onResultListener) {
        mProxyHost = proxyHost;
        mProxyPort = proxyPort;
        mOnResultListenerRef = new WeakReference<>(onResultListener);
    }

    @Override
    protected TestResult doInBackground(String... strings) {
        String testUrl = strings[0];
        try {
            long startTime = System.currentTimeMillis();
            InetSocketAddress proxyAddress = new InetSocketAddress(mProxyHost, (int) mProxyPort);
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, proxyAddress);
            URLConnection connection = new URL(testUrl).openConnection(proxy);
            connection.setConnectTimeout(DEFAULT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_TIMEOUT);
            connection.connect();
            return new TestResult(testUrl, true, "",
                    System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            return new TestResult(testUrl, false, e.getMessage(), 0);
        }
    }

    @Override
    protected void onPostExecute(TestResult testResult) {
        OnResultListener listener = mOnResultListenerRef.get();
        if (listener != null) {
            listener.onResult(testResult.url, testResult.connected, testResult.delay, testResult.error);
        }
    }

    public interface OnResultListener {
        void onResult(String testUrl, boolean connected, long delay, String error);
    }
}

class TestResult {
    boolean connected;
    String url;
    String error;
    long delay;

    TestResult(String url, boolean connected, @NonNull String error, long delay) {
        this.connected = connected;
        this.url = url;
        this.error = error;
        this.delay = delay;
    }
}
