package io.github.trojan_gfw.igniter.connection;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import io.github.trojan_gfw.igniter.ProxyService;
import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.proxy.aidl.ITrojanService;
import io.github.trojan_gfw.igniter.proxy.aidl.ITrojanServiceCallback;

/**
 * A class that delegates interaction with {@link ProxyService}. You should call {@link #connect(Context, Callback)}
 * when you are ready for interacting with {@link ProxyService} and call {@link #disconnect(Context)}
 * in the end. {@link TrojanConnection} would bind {@link ProxyService} and register {@link ITrojanServiceCallback}.
 * You can easily obtain {@link ProxyService} and get state change as well as connection test result
 * by implementing {@link Callback}.
 *
 * @see ProxyService
 * @see ITrojanService
 * @see ITrojanServiceCallback
 */
public class TrojanConnection implements ServiceConnection, Binder.DeathRecipient {
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private ITrojanService mTrojanService;
    private Callback mCallback;
    private boolean mServiceCallbackRegistered;
    private final boolean mListenToDeath;
    private boolean mAlreadyConnected;
    private IBinder mBinder;
    /**
     * Implementation of {@link ITrojanServiceCallback}. The callback is registered in {@link #onServiceConnected(ComponentName, IBinder)},
     * and unregistered in {@link #onServiceDisconnected(ComponentName)}. The callback is considered
     * to be invoked by {@link ITrojanService}, in this case, a field of {@link ProxyService} implements
     * {@link ITrojanService}.
     *
     * @see ProxyService
     * @see ITrojanService
     */
    private ITrojanServiceCallback mTrojanServiceCallback = new ITrojanServiceCallback.Stub() {
        @Override
        public void onStateChanged(final int state, final String msg) throws RemoteException {
            if (mCallback != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onStateChanged(state, msg);
                    }
                });
            }
        }

        @Override
        public void onTestResult(final String testUrl, final boolean connected, final long delay, final String error) throws RemoteException {
            if (mCallback != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onTestResult(testUrl, connected, delay, error);
                    }
                });
            }
        }
    };

    public TrojanConnection(boolean listenToDeath) {
        super();
        mListenToDeath = listenToDeath;
    }

    /**
     * Callback for events that are relative to {@link ProxyService}.
     */
    public interface Callback {
        void onServiceConnected(ITrojanService service);

        void onServiceDisconnected();

        void onStateChanged(int state, String msg);

        void onTestResult(String testUrl, boolean connected, long delay, @NonNull String error);

        void onBinderDied();
    }

    public void connect(Context context, Callback callback) {
        if (mAlreadyConnected) {
            return;
        }
        mAlreadyConnected = true;
        if (mCallback != null) {
            throw new IllegalStateException("Required to call disconnect(Context) first.");
        }
        mCallback = callback;

        // todo: choose the service class dynamically.
        Intent intent = new Intent(context, ProxyService.class);
        intent.setAction(context.getString(R.string.bind_service));
        context.bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    public void disconnect(Context context) {
        unregisterServiceCallback();
        if (mAlreadyConnected) {
            try {
                context.unbindService(this);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            mAlreadyConnected = false;
            if (mListenToDeath && mBinder != null) {
                mBinder.unlinkToDeath(this, 0);
            }
            mBinder = null;
            mTrojanService = null;
            mCallback = null;
        }
    }

    private void unregisterServiceCallback() {
        ITrojanService service = mTrojanService;
        if (service != null && mServiceCallbackRegistered) {
            try {
                service.unregisterCallback(mTrojanServiceCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mServiceCallbackRegistered = false;
        }
    }

    public ITrojanService getService() {
        return mTrojanService;
    }

    /**
     * Obtain the binder {@link ITrojanService} returned by {@link ProxyService#onBind(Intent)} and
     * register callback {@link #mTrojanServiceCallback} with the binder.
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        mBinder = binder;
        ITrojanService service = ITrojanService.Stub.asInterface(binder);
        mTrojanService = service;
        try {
            if (mListenToDeath) {
                binder.linkToDeath(this, 0);
            }
            if (mServiceCallbackRegistered) {
                throw new IllegalStateException("TrojanServiceCallback already registered!");
            }
            service.registerCallback(mTrojanServiceCallback);
            mServiceCallbackRegistered = true;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (mCallback != null) {
            mCallback.onServiceConnected(service);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        unregisterServiceCallback();
        if (mCallback != null) {
            mCallback.onServiceDisconnected();
        }
        mTrojanService = null;
        mBinder = null;
    }

    @Override
    public void binderDied() {
        mTrojanService = null;
        mServiceCallbackRegistered = false;
        if (mCallback != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onBinderDied();
                }
            });
        }
    }
}
