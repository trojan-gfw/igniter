package io.github.trojan_gfw.igniter.tile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import io.github.trojan_gfw.igniter.Globals;
import io.github.trojan_gfw.igniter.MainActivity;
import io.github.trojan_gfw.igniter.ProxyService;
import io.github.trojan_gfw.igniter.R;
import io.github.trojan_gfw.igniter.TrojanConfig;
import io.github.trojan_gfw.igniter.TrojanHelper;
import io.github.trojan_gfw.igniter.common.os.MultiProcessSP;

/**
 * {@link ProxyControlActivity} provides convenient access to start/stop {@link ProxyService}.
 * Noticed that {@link ProxyControlActivity} runs in the same process as {@link ProxyService} does.
 * <br/>
 * When you start this activity with an intent created by {@link #startOrStopProxy(Context, boolean, boolean)},
 * {@link ProxyControlActivity} will start {@link ProxyService} by {@link #startForegroundService(Intent)}
 * and stop it by sending broadcast with action {@link R.string#stop_service}.
 *
 * @see ProxyService
 * @see R.string#stop_service
 */
public class ProxyControlActivity extends AppCompatActivity {
    private static final String TAG = "ProxyControlActivity";
    private static final String EXTRA_ENABLE_VPN = "en_vpn";
    private static final int VPN_REQUEST_CODE = 0;

    /**
     * Create an Intent to start or stop {@link ProxyService}
     *
     * @param context   Context
     * @param enableVPN start or stop ProxyService
     * @param fromActivity whether this Activity will be started in an Activity. If this Activity
     *                     won't be started in an Activity, {@link Intent#FLAG_ACTIVITY_NEW_TASK} should
     *                     be set in the Intent.
     * @return Intent
     */
    public static Intent startOrStopProxy(Context context, boolean enableVPN, boolean fromActivity) {
        Intent intent = new Intent(context, ProxyControlActivity.class);
        if (!fromActivity) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.putExtra(EXTRA_ENABLE_VPN, enableVPN);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        moveTaskToBack(true);
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(EXTRA_ENABLE_VPN)) {
                boolean startVPN = intent.getBooleanExtra(EXTRA_ENABLE_VPN, false);
                if (startVPN) {
                    startVPN();
                } else {
                    stopVPN();
                    finish();
                }
            }  else {
                // if intent has no extra 'EXTRA_ENABLE_VPN', the activity must
                // be started by a long click on tile. In this case we just simply start the Launcher Activity.
                startLauncherActivity();
                finish();
            }
        } else {
            startLauncherActivity();
            finish();
        }
    }

    private void startLauncherActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void showStartVPNError() {
        Toast.makeText(getApplicationContext(),
                R.string.invalid_configuration,
                Toast.LENGTH_LONG).show();
    }

    private void startVPN() {
        TrojanConfig config = Globals.getTrojanConfigInstance();
        TrojanConfig cacheConfig = TrojanHelper.readTrojanConfig(Globals.getTrojanConfigPath());
        if (cacheConfig == null) {
            showStartVPNError();
            finish();
            return;
        } else {
            config.copyFrom(cacheConfig);
        }
        config.setCaCertPath(Globals.getCaCertPath());
        TrojanHelper.ShowConfig(Globals.getTrojanConfigPath());
        if (!Globals.getTrojanConfigInstance().isValidRunningConfig()) {
            showStartVPNError();
            finish();
            return;
        }
        Intent i = VpnService.prepare(getApplicationContext());
        if (i != null) {
            startActivityForResult(i, VPN_REQUEST_CODE);
        } else {
            onActivityResult(VPN_REQUEST_CODE, Activity.RESULT_OK, null);
        }
    }

    private void stopVPN() {
        Intent intent = new Intent(getString(R.string.stop_service));
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Intent intent = new Intent(this, ProxyService.class);
                intent.putExtra(ProxyService.CLASH_EXTRA_NAME, MultiProcessSP.getEnableClash(true));
                ContextCompat.startForegroundService(this, intent);
            }
        }
        finish();
    }
}
