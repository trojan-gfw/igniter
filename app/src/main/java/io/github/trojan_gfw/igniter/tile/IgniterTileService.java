package io.github.trojan_gfw.igniter.tile;

import android.content.Intent;
import android.os.Build;
import android.os.RemoteException;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import io.github.trojan_gfw.igniter.LogHelper;
import io.github.trojan_gfw.igniter.MainActivity;
import io.github.trojan_gfw.igniter.ProxyService;
import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.common.os.MultiProcessSP;
import io.github.trojan_gfw.igniter.connection.TrojanConnection;
import io.github.trojan_gfw.igniter.proxy.aidl.ITrojanService;

/**
 * Igniter's implementation of TileService, showing current state of {@link ProxyService} and providing a
 * shortcut to start or stop {@link ProxyService} by the help of {@link ProxyControlActivity}. This
 * service receives state change by the help of {@link TrojanConnection}.
 *
 * @see ProxyService
 * @see io.github.trojan_gfw.igniter.ProxyService.ProxyState
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class IgniterTileService extends TileService implements TrojanConnection.Callback {
    private static final String TAG = "IgniterTile";
    private final TrojanConnection mConnection = new TrojanConnection(false);
    /**
     * Indicates that user had tapped the tile before {@link TrojanConnection} connects {@link ProxyService}.
     * Generally speaking, when the connection is built, we should call {@link #onClick()} again if
     * the value is <code>true</code>.
     */
    private boolean mTapPending;

    @Override
    public void onStartListening() {
        super.onStartListening();
        LogHelper.i(TAG, "onStartListening");
        mConnection.connect(this, this);
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        LogHelper.i(TAG, "onStopListening");
        mConnection.disconnect(this);
    }

    @Override
    public void onServiceConnected(ITrojanService service) {
        LogHelper.i(TAG, "onServiceConnected");
        try {
            int state = service.getState();
            updateTile(state);
            if (mTapPending) {
                mTapPending = false;
                onClick();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceDisconnected() {
        LogHelper.i(TAG, "onServiceDisconnected");
    }

    @Override
    public void onStateChanged(int state, String msg) {
        LogHelper.i(TAG, "onStateChanged# state: " + state + ", msg: " + msg);
        updateTile(state);
    }

    @Override
    public void onTestResult(String testUrl, boolean connected, long delay, @NonNull String error) {
        // Do nothing, since TileService will not submit test request.
    }

    @Override
    public void onBinderDied() {
        LogHelper.i(TAG, "onBinderDied");
    }

    private void updateTile(final @ProxyService.ProxyState int state) {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        LogHelper.i(TAG, "updateTile with state: " + state);
        switch (state) {
            case ProxyService.STATE_NONE:
                tile.setState(Tile.STATE_INACTIVE);
                tile.setLabel(getString(R.string.app_name));
                break;
            case ProxyService.STARTED:
                tile.setState(Tile.STATE_ACTIVE);
                tile.setLabel(getString(R.string.tile_on));
                break;
            case ProxyService.STARTING:
                tile.setState(Tile.STATE_ACTIVE);
                tile.setLabel(getString(R.string.tile_starting));
                break;
            case ProxyService.STOPPED:
                tile.setState(Tile.STATE_INACTIVE);
                tile.setLabel(getString(R.string.tile_off));
                break;
            case ProxyService.STOPPING:
                tile.setState(Tile.STATE_INACTIVE);
                tile.setLabel(getString(R.string.tile_stopping));
                break;
            default:
                LogHelper.e(TAG, "Unknown state: " + state);
                break;
        }
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        LogHelper.i(TAG, "onClick");
        if (MultiProcessSP.isFirstStart(true)) {
            // if user never open Igniter before, when he/she clicks the tile, it is necessary
            // to start the launcher activity for resource preparation.
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }
        ITrojanService service = mConnection.getService();
        if (service == null) {
            mTapPending = true;
        } else {
            try {
                @ProxyService.ProxyState int state = service.getState();
                switch (state) {
                    case ProxyService.STARTED: {
                        startActivity(ProxyControlActivity.startOrStopProxy(this, false, false));
                        break;
                    }
                    case ProxyService.STARTING:
                    case ProxyService.STOPPING:
                        break;
                    case ProxyService.STATE_NONE:
                    case ProxyService.STOPPED: {
                        startActivity(ProxyControlActivity.startOrStopProxy(this, true, false));
                        break;
                    }
                    default:
                        LogHelper.e(TAG, "Unknown state: " + state);
                        break;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
