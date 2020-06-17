package io.github.trojan_gfw.igniter;


import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import io.github.trojan_gfw.igniter.common.constants.Constants;
import io.github.trojan_gfw.igniter.common.os.Task;
import io.github.trojan_gfw.igniter.common.os.Threads;
import io.github.trojan_gfw.igniter.common.utils.PreferenceUtils;
import io.github.trojan_gfw.igniter.common.utils.SnackbarUtils;
import io.github.trojan_gfw.igniter.connection.TrojanConnection;
import io.github.trojan_gfw.igniter.exempt.activity.ExemptAppActivity;
import io.github.trojan_gfw.igniter.proxy.aidl.ITrojanService;
import io.github.trojan_gfw.igniter.servers.activity.ServerListActivity;
import io.github.trojan_gfw.igniter.servers.data.ServerListDataManager;
import io.github.trojan_gfw.igniter.servers.data.ServerListDataSource;
import io.github.trojan_gfw.igniter.tile.ProxyHelper;


public class MainActivity extends AppCompatActivity implements TrojanConnection.Callback {
    private static final String TAG = "MainActivity";
    private static final int READ_WRITE_EXT_STORAGE_PERMISSION_REQUEST = 514;
    private static final int VPN_REQUEST_CODE = 233;
    private static final int SERVER_LIST_CHOOSE_REQUEST_CODE = 1024;
    private static final int EXEMPT_APP_CONFIGURE_REQUEST_CODE = 2077;
    private static final String CONNECTION_TEST_URL = "https://www.google.com";

    private String shareLink;
    private ViewGroup rootViewGroup;
    private EditText remoteServerRemarkText;
    private EditText remoteAddrText;
    private EditText remotePortText;
    private EditText passwordText;
    private Switch ipv6Switch;
    private Switch verifySwitch;
    private Switch clashSwitch;
    private TextView clashLink;
    private Button startStopButton;
    private EditText trojanURLText;
    private @ProxyService.ProxyState
    int proxyState = ProxyService.STATE_NONE;
    private final TrojanConnection connection = new TrojanConnection(false);
    private ITrojanService trojanService;
    private ServerListDataSource serverListDataManager;
    private AlertDialog linkDialog;

    private TextViewListener remoteServerRemarkTextListener = new TextViewListener() {
        @Override
        protected void onTextChanged(String before, String old, String aNew, String after) {
            // update TextView
            startUpdates(); // to prevent infinite loop.
            if (remoteServerRemarkText.hasFocus()) {
                TrojanConfig ins = Globals.getTrojanConfigInstance();
                ins.setRemoteServerRemark(remoteServerRemarkText.getText().toString());
            }
            endUpdates();
        }
    };

    private TextViewListener remoteAddrTextListener = new TextViewListener() {
        @Override
        protected void onTextChanged(String before, String old, String aNew, String after) {
            // update TextView
            startUpdates(); // to prevent infinite loop.
            if (remoteAddrText.hasFocus()) {
                TrojanConfig ins = Globals.getTrojanConfigInstance();
                ins.setRemoteAddr(remoteAddrText.getText().toString());
            }
            endUpdates();
        }
    };
    private TextViewListener remotePortTextListener = new TextViewListener() {
        @Override
        protected void onTextChanged(String before, String old, String aNew, String after) {
            // update TextView
            startUpdates(); // to prevent infinite loop.
            if (remotePortText.hasFocus()) {
                TrojanConfig ins = Globals.getTrojanConfigInstance();
                String portStr = remotePortText.getText().toString();
                try {
                    int port = Integer.parseInt(portStr);
                    ins.setRemotePort(port);
                } catch (NumberFormatException e) {
                    // Ignore when we get invalid number
                    e.printStackTrace();
                }
            }
            endUpdates();
        }
    };
    private TextViewListener passwordTextListener = new TextViewListener() {
        @Override
        protected void onTextChanged(String before, String old, String aNew, String after) {
            // update TextView
            startUpdates(); // to prevent infinite loop.
            if (passwordText.hasFocus()) {
                TrojanConfig ins = Globals.getTrojanConfigInstance();
                ins.setPassword(passwordText.getText().toString());
            }
            endUpdates();
        }
    };

    private void copyRawResourceToDir(int resId, String destPathName, boolean override) {
        File file = new File(destPathName);
        if (override || !file.exists()) {
            try {
                try (InputStream is = getResources().openRawResource(resId);
                     FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = is.read(buf)) > 0) {
                        fos.write(buf, 0, len);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateViews(int state) {
        proxyState = state;
        boolean inputEnabled;
        switch (state) {
            case ProxyService.STARTING: {
                inputEnabled = false;
                startStopButton.setText(R.string.button_service__starting);
                startStopButton.setEnabled(false);
                break;
            }
            case ProxyService.STARTED: {
                inputEnabled = false;
                startStopButton.setText(R.string.button_service__stop);
                startStopButton.setEnabled(true);
                break;
            }
            case ProxyService.STOPPING: {
                inputEnabled = false;
                startStopButton.setText(R.string.button_service__stopping);
                startStopButton.setEnabled(false);
                break;
            }
            default: {
                inputEnabled = true;
                startStopButton.setText(R.string.button_service__start);
                startStopButton.setEnabled(true);
                break;
            }
        }
        remoteServerRemarkText.setEnabled(inputEnabled);
        remoteAddrText.setEnabled(inputEnabled);
        remotePortText.setEnabled(inputEnabled);
        ipv6Switch.setEnabled(inputEnabled);
        passwordText.setEnabled(inputEnabled);
        verifySwitch.setEnabled(inputEnabled);
        clashSwitch.setEnabled(inputEnabled);
        clashLink.setEnabled(inputEnabled);
    }

    private void applyConfigString(String configString) {
        TrojanConfig ins = Globals.getTrojanConfigInstance();
        TrojanConfig parsedConfig = TrojanURLHelper.ParseTrojanURL(configString);
        if (parsedConfig != null) {
            String remoteServerRemark = parsedConfig.getRemoteServerRemark();
            String remoteAddress = parsedConfig.getRemoteAddr();
            int remotePort = parsedConfig.getRemotePort();
            String password = parsedConfig.getPassword();

            ins.setRemoteServerRemark(remoteServerRemark);
            ins.setRemoteAddr(remoteAddress);
            ins.setRemotePort(remotePort);
            ins.setPassword(password);

            remoteServerRemarkText.setText(remoteServerRemark);
            passwordText.setText(password);
            remotePortText.setText(String.valueOf(remotePort));
            remoteAddrText.setText(remoteAddress);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rootViewGroup = findViewById(R.id.rootScrollView);
        remoteServerRemarkText = findViewById(R.id.remoteServerRemarkText);
        remoteAddrText = findViewById(R.id.remoteAddrText);
        remotePortText = findViewById(R.id.remotePortText);
        passwordText = findViewById(R.id.passwordText);
        ipv6Switch = findViewById(R.id.ipv6Switch);
        verifySwitch = findViewById(R.id.verifySwitch);
        clashSwitch = findViewById(R.id.clashSwitch);
        clashLink = findViewById(R.id.clashLink);
        clashLink.setMovementMethod(LinkMovementMethod.getInstance());
        startStopButton = findViewById(R.id.startStopButton);

        copyRawResourceToDir(R.raw.cacert, Globals.getCaCertPath(), true);
        copyRawResourceToDir(R.raw.country, Globals.getCountryMmdbPath(), true);
        copyRawResourceToDir(R.raw.clash_config, Globals.getClashConfigPath(), false);

        remoteServerRemarkText.addTextChangedListener(remoteServerRemarkTextListener);

        remoteAddrText.addTextChangedListener(remoteAddrTextListener);

        remotePortText.addTextChangedListener(remotePortTextListener);

        passwordText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    passwordText.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    // place cursor on the end
                    passwordText.setInputType(EditorInfo.TYPE_CLASS_TEXT);
                    passwordText.setSelection(passwordText.getText().length());
                }
            }
        });

        boolean enableClash = PreferenceUtils.getBooleanPreference(getContentResolver(),
                Uri.parse(Constants.PREFERENCE_URI), Constants.PREFERENCE_KEY_ENABLE_CLASH, true);
        clashSwitch.setChecked(enableClash);
        clashSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Generally speaking, it's better to insert content into ContentProvider in background
                // thread, but that may cause data inconsistency when user starts proxy right after
                // switching.
                PreferenceUtils.putBooleanPreference(getContentResolver(),
                        Uri.parse(Constants.PREFERENCE_URI), Constants.PREFERENCE_KEY_ENABLE_CLASH,
                        isChecked);
            }
        });

        passwordText.addTextChangedListener(passwordTextListener);

        ipv6Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TrojanConfig ins = Globals.getTrojanConfigInstance();
                ins.setEnableIpv6(isChecked);
            }
        });

        verifySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TrojanConfig ins = Globals.getTrojanConfigInstance();
                ins.setVerifyCert(isChecked);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Trojan URL");

        trojanURLText = new EditText(this);

        trojanURLText.setInputType(InputType.TYPE_CLASS_TEXT);
        trojanURLText.setSingleLine(false);
        trojanURLText.setSelectAllOnFocus(true);

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        trojanURLText.setLayoutParams(params);
        container.addView(trojanURLText);
        builder.setView(container);

        builder.setPositiveButton(R.string.common_update, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                applyConfigString(trojanURLText.getText().toString());
                dialog.cancel();
            }
        });
        builder.setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        linkDialog = builder.create();

        TextViewListener trojanConfigChangedTextViewListener = new TextViewListener() {
            @Override
            protected void onTextChanged(String before, String old, String aNew, String after) {
                startUpdates();
                String str = TrojanURLHelper.GenerateTrojanURL(Globals.getTrojanConfigInstance());
                if (str != null) {
                    shareLink = str;
                }
                endUpdates();
            }
        };

        remoteAddrText.addTextChangedListener(trojanConfigChangedTextViewListener);
        remotePortText.addTextChangedListener(trojanConfigChangedTextViewListener);
        passwordText.addTextChangedListener(trojanConfigChangedTextViewListener);

        startStopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!Globals.getTrojanConfigInstance().isValidRunningConfig()) {
                    Toast.makeText(MainActivity.this,
                            R.string.invalid_configuration,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (proxyState == ProxyService.STATE_NONE || proxyState == ProxyService.STOPPED) {
                    TrojanHelper.WriteTrojanConfig(
                            Globals.getTrojanConfigInstance(),
                            Globals.getTrojanConfigPath()
                    );
                    TrojanHelper.ShowConfig(Globals.getTrojanConfigPath());
                    // start ProxyService
                    Intent i = VpnService.prepare(getApplicationContext());
                    if (i != null) {
                        startActivityForResult(i, VPN_REQUEST_CODE);
                    } else {
                        ProxyHelper.startProxyService(getApplicationContext());
                    }
                } else if (proxyState == ProxyService.STARTED) {
                    // stop ProxyService
                    ProxyHelper.stopProxyService(getApplicationContext());
                }
            }
        });
        serverListDataManager = new ServerListDataManager(Globals.getTrojanConfigListPath());
        connection.connect(this, this);
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                PreferenceUtils.putBooleanPreference(getContentResolver(),
                        Uri.parse(Constants.PREFERENCE_URI),
                        Constants.PREFERENCE_KEY_FIRST_START, false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkTrojanURLFromClipboard();
    }

    private void checkTrojanURLFromClipboard() {
        Threads.instance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (!clipboardManager.hasPrimaryClip()) {
                    return;
                }
                ClipData clipData = clipboardManager.getPrimaryClip();
                // check clipboard
                if (clipData == null || clipData.getItemCount() == 0) {
                    return;
                }
                final CharSequence clipboardText = clipData.getItemAt(0).coerceToText(MainActivity.this);
                // check scheme
                TrojanConfig config = TrojanURLHelper.ParseTrojanURL(clipboardText.toString());
                if (config == null) {
                    return;
                }

                // show once if trojan url
                if (clipboardManager.hasPrimaryClip()) {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""));
                }
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(R.string.clipboard_import_tip)
                        .setPositiveButton(R.string.common_confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                applyConfigString(clipboardText.toString());
                            }
                        })
                        .setNegativeButton(R.string.common_cancel, null)
                        .create()
                        .show();
            }
        });
    }

    @Override
    public void onServiceConnected(final ITrojanService service) {
        LogHelper.i(TAG, "onServiceConnected");
        trojanService = service;
        Threads.instance().runOnWorkThread(new Task() {
            @Override
            public void onRun() {
                try {
                    final int state = service.getState();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateViews(state);
                        }
                    });
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onServiceDisconnected() {
        LogHelper.i(TAG, "onServiceConnected");
        trojanService = null;
    }

    @Override
    public void onStateChanged(int state, String msg) {
        LogHelper.i(TAG, "onStateChanged# state: " + state + " msg: " + msg);
        updateViews(state);
    }

    @Override
    public void onTestResult(final String testUrl, final boolean connected, final long delay, @NonNull final String error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showTestConnectionResult(testUrl, connected, delay, error);
            }
        });
    }

    private void showTestConnectionResult(String testUrl, boolean connected, long delay, @NonNull String error) {
        if (connected) {
            Toast.makeText(getApplicationContext(), getString(R.string.connected_to__in__ms,
                    testUrl, String.valueOf(delay)), Toast.LENGTH_LONG).show();
        } else {
            LogHelper.e(TAG, "TestError: " + error);
            Toast.makeText(getApplicationContext(),
                    getString(R.string.failed_to_connect_to__,
                            testUrl, error),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBinderDied() {
        LogHelper.i(TAG, "onBinderDied");
        connection.disconnect(this);
        // connect the new binder
        // todo is it necessary to re-connect?
        connection.connect(this, this);
    }

    /**
     * Test connection by invoking {@link ITrojanService#testConnection(String)}. Since {@link ITrojanService}
     * is from remote process, a {@link RemoteException} might be thrown. Test result will be delivered
     * to {@link #onTestResult(String, boolean, long, String)} by {@link TrojanConnection}.
     */
    private void testConnection() {
        ITrojanService service = trojanService;
        if (service == null) {
            showTestConnectionResult(CONNECTION_TEST_URL, false, 0L, getString(R.string.trojan_service_not_available));
        } else {
            try {
                service.testConnection(CONNECTION_TEST_URL);
            } catch (RemoteException e) {
                showTestConnectionResult(CONNECTION_TEST_URL, false, 0L, getString(R.string.trojan_service_error));
                e.printStackTrace();
            }
        }
    }

    /**
     * Show develop info in Logcat by invoking {@link ITrojanService#showDevelopInfoInLogcat}. Since {@link ITrojanService}
     * is from remote process, a {@link RemoteException} might be thrown.
     */
    private void showDevelopInfoInLogcat() {
        ITrojanService service = trojanService;
        if (service != null) {
            try {
                service.showDevelopInfoInLogcat();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void clearEditTextFocus() {
        remoteServerRemarkText.clearFocus();
        remoteAddrText.clearFocus();
        remotePortText.clearFocus();
        passwordText.clearFocus();
    }

    private void showSaveConfigResult(final boolean success) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),
                        success ? R.string.main_save_success : R.string.main_save_failed,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (SERVER_LIST_CHOOSE_REQUEST_CODE == requestCode && resultCode == Activity.RESULT_OK && data != null) {
            shareLink = "";
            final TrojanConfig config = data.getParcelableExtra(ServerListActivity.KEY_TROJAN_CONFIG);
            if (config != null) {
                config.setCaCertPath(Globals.getCaCertPath());
                Globals.setTrojanConfigInstance(config);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        remoteServerRemarkText.setText(config.getRemoteServerRemark());
                        remoteAddrText.setText(config.getRemoteAddr());
                        remotePortText.setText(String.valueOf(config.getRemotePort()));
                        passwordText.setText(config.getPassword());
                        TrojanHelper.WriteTrojanConfig(Globals.getTrojanConfigInstance(), Globals.getTrojanConfigPath());
                    }
                });
                shareLink = TrojanURLHelper.GenerateTrojanURL(config);
                ipv6Switch.setChecked(config.getEnableIpv6());
                verifySwitch.setChecked(config.getVerifyCert());
            }
        } else if (EXEMPT_APP_CONFIGURE_REQUEST_CODE == requestCode && Activity.RESULT_OK == resultCode) {
            if (ProxyService.STARTED == proxyState) {
                SnackbarUtils.showTextLong(rootViewGroup, R.string.main_restart_proxy_service_tip);
            }
        } else if (VPN_REQUEST_CODE == requestCode && RESULT_OK == resultCode) {
            ProxyHelper.startProxyService(getApplicationContext());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Bind menu items to their relative actions
        switch (item.getItemId()) {
            case R.id.action_test_connection:
                testConnection();
                return true;
            case R.id.action_show_develop_info_logcat:
                // log of this process
                LogHelper.showDevelopInfoInLogcat();
                // log of other processes
                showDevelopInfoInLogcat();
                return true;
            case R.id.action_save_profile:
                if (!Globals.getTrojanConfigInstance().isValidRunningConfig()) {
                    Toast.makeText(MainActivity.this, R.string.invalid_configuration, Toast.LENGTH_SHORT).show();
                    return true;
                }
                Threads.instance().runOnWorkThread(new Task() {
                    @Override
                    public void onRun() {
                        TrojanConfig config = Globals.getTrojanConfigInstance();
                        TrojanHelper.WriteTrojanConfig(config, Globals.getTrojanConfigPath());
                        serverListDataManager.saveServerConfig(config);
                        showSaveConfigResult(true);
                    }
                });
                return true;
            case R.id.action_view_server_list:
                clearEditTextFocus();
                startActivityForResult(ServerListActivity.create(MainActivity.this), SERVER_LIST_CHOOSE_REQUEST_CODE);
                return true;
            case R.id.action_about:
                clearEditTextFocus();
                startActivity(AboutActivity.create(MainActivity.this));
                return true;
            case R.id.action_share_link:
                trojanURLText.setText(shareLink);
                linkDialog.show();
                trojanURLText.selectAll();
                return true;
            case R.id.action_exempt_app:
                startActivityForResult(ExemptAppActivity.create(this), EXEMPT_APP_CONFIGURE_REQUEST_CODE);
                return true;
            default:
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        File file = new File(Globals.getTrojanConfigPath());
        if (file.exists()) {
            try {
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] content = new byte[(int) file.length()];
                    fis.read(content);
                    String contentStr = new String(content);
                    TrojanConfig ins = Globals.getTrojanConfigInstance();
                    ins.fromJSON(contentStr);

                    remoteServerRemarkText.setText(ins.getRemoteServerRemark());
                    remoteAddrText.setText(ins.getRemoteAddr());
                    remotePortText.setText(String.valueOf(ins.getRemotePort()));
                    passwordText.setText(ins.getPassword());
                    ipv6Switch.setChecked(ins.getEnableIpv6());
                    verifySwitch.setChecked(ins.getVerifyCert());
                    remoteAddrText.setSelection(remoteAddrText.length());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connection.disconnect(this);
    }
}
