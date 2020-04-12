package io.github.trojan_gfw.igniter;


import android.Manifest;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import io.github.trojan_gfw.igniter.common.os.MultiProcessSP;
import io.github.trojan_gfw.igniter.common.os.Task;
import io.github.trojan_gfw.igniter.common.os.Threads;
import io.github.trojan_gfw.igniter.common.utils.PermissionUtils;
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

    private ViewGroup rootViewGroup;
    private EditText remoteAddrText;
    private EditText remotePortText;
    private EditText passwordText;
    private Switch ipv6Switch;
    private Switch verifySwitch;
    private Switch clashSwitch;
    private TextView clashLink;
    private Button startStopButton;
    private EditText trojanURLText;
    private Handler mHandler = new Handler();
    private @ProxyService.ProxyState
    int proxyState = ProxyService.STATE_NONE;
    private final TrojanConnection connection = new TrojanConnection(false);
    private ITrojanService trojanService;
    private ServerListDataSource serverListDataManager;
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
        remoteAddrText.setEnabled(inputEnabled);
        remotePortText.setEnabled(inputEnabled);
        ipv6Switch.setEnabled(inputEnabled);
        passwordText.setEnabled(inputEnabled);
        trojanURLText.setEnabled(inputEnabled);
        verifySwitch.setEnabled(inputEnabled);
        clashSwitch.setEnabled(inputEnabled);
        clashLink.setEnabled(inputEnabled);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rootViewGroup = findViewById(R.id.rootScrollView);
        Button saveServerIb = findViewById(R.id.saveConfigBtn);
        remoteAddrText = findViewById(R.id.remoteAddrText);
        remotePortText = findViewById(R.id.remotePortText);
        passwordText = findViewById(R.id.passwordText);
        trojanURLText = findViewById(R.id.trojanURLText);
        ipv6Switch = findViewById(R.id.ipv6Switch);
        verifySwitch = findViewById(R.id.verifySwitch);
        clashSwitch = findViewById(R.id.clashSwitch);
        clashLink = findViewById(R.id.clashLink);
        clashLink.setMovementMethod(LinkMovementMethod.getInstance());
        startStopButton = findViewById(R.id.startStopButton);

        copyRawResourceToDir(R.raw.cacert, Globals.getCaCertPath(), true);
        copyRawResourceToDir(R.raw.country, Globals.getCountryMmdbPath(), true);
        copyRawResourceToDir(R.raw.clash_config, Globals.getClashConfigPath(), false);

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

        clashSwitch.setChecked(MultiProcessSP.getEnableClash(true));
        clashSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MultiProcessSP.setEnableClash(isChecked);
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

        trojanURLText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                trojanURLText.selectAll();
                return false;
            }
        });

        trojanURLText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    trojanURLText.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    // it seems we don't have to place cursor on the end for Trojan URL
                    trojanURLText.setInputType(EditorInfo.TYPE_CLASS_TEXT);
                }
            }
        });

        trojanURLText.addTextChangedListener(new TextViewListener() {
            @Override
            protected void onTextChanged(String before, String old, String aNew, String after) {
                // update TextView
                startUpdates(); // to prevent infinite loop.
                TrojanConfig ins = Globals.getTrojanConfigInstance();
                TrojanConfig parsedConfig = TrojanURLHelper.ParseTrojanURL(before + aNew + after);
                if (parsedConfig != null) {
                    String remoteAddress = parsedConfig.getRemoteAddr();
                    int remotePort = parsedConfig.getRemotePort();
                    String password = parsedConfig.getPassword();

                    ins.setRemoteAddr(remoteAddress);
                    ins.setRemotePort(remotePort);
                    ins.setPassword(password);
                }
                endUpdates();
            }
        });

        TextViewListener trojanConfigChangedTextViewListener = new TextViewListener() {
            @Override
            protected void onTextChanged(String before, String old, String aNew, String after) {
                startUpdates();
                String str = TrojanURLHelper.GenerateTrojanURL(Globals.getTrojanConfigInstance());
                if (str != null) {
                    trojanURLText.setText(str);
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
        saveServerIb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Globals.getTrojanConfigInstance().isValidRunningConfig()) {
                    Toast.makeText(MainActivity.this, R.string.invalid_configuration, Toast.LENGTH_SHORT).show();
                    return;
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
            }
        });
        serverListDataManager = new ServerListDataManager(Globals.getTrojanConfigListPath());
        connection.connect(this, this);
        if (!PermissionUtils.hasReadWriteExtStoragePermission(this) && ActivityCompat
                .shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestReadWriteExternalStoragePermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // use handler to getPrimaryClip
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ClipboardManager mClipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                // check clipboard
                if (!mClipboardManager.hasPrimaryClip() || mClipboardManager.getPrimaryClip().getItemCount() == 0) {
                    return;
                }

                final CharSequence clipboardText = mClipboardManager.getPrimaryClip().getItemAt(0).getText();
                // check scheme
                if (!"trojan".equals(Uri.parse(clipboardText.toString()).getScheme())) {
                    return;
                }
                // show once if trojan url
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    mClipboardManager.clearPrimaryClip();
                }
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(R.string.clipboard_import_tip)
                        .setPositiveButton(R.string.common_confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                trojanURLText.setText(clipboardText);
                            }
                        })
                        .setNegativeButton(R.string.common_cancel, null)
                        .create()
                        .show();
            }
        });
    }

    private void requestReadWriteExternalStoragePermission() {
        new AlertDialog.Builder(this).setTitle(R.string.common_alert)
                .setMessage(R.string.main_write_external_storage_permission_requirement)
                .setPositiveButton(R.string.common_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        }, READ_WRITE_EXT_STORAGE_PERMISSION_REQUEST);
                    }
                })
                .setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
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
            showTestConnectionResult(CONNECTION_TEST_URL, false, 0L, "Trojan service is not available.");
        } else {
            try {
                service.testConnection(CONNECTION_TEST_URL);
            } catch (RemoteException e) {
                showTestConnectionResult(CONNECTION_TEST_URL, false, 0L, "Trojan service throws RemoteException.");
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
        remoteAddrText.clearFocus();
        remotePortText.clearFocus();
        passwordText.clearFocus();
        trojanURLText.clearFocus();
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
            trojanURLText.setText("");
            final TrojanConfig config = data.getParcelableExtra(ServerListActivity.KEY_TROJAN_CONFIG);
            if (config != null) {
                config.setCaCertPath(Globals.getCaCertPath());
                Globals.setTrojanConfigInstance(config);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        remoteAddrText.setText(config.getRemoteAddr());
                        remotePortText.setText(String.valueOf(config.getRemotePort()));
                        passwordText.setText(config.getPassword());
                    }
                });
                trojanURLText.setText(TrojanURLHelper.GenerateTrojanURL(config));
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
            case R.id.action_view_server_list:
                clearEditTextFocus();
                startActivityForResult(ServerListActivity.create(MainActivity.this), SERVER_LIST_CHOOSE_REQUEST_CODE);
                return true;
            case R.id.action_exempt_app:
                if (PermissionUtils.hasReadWriteExtStoragePermission(this)) {
                    startActivityForResult(ExemptAppActivity.create(this), EXEMPT_APP_CONFIGURE_REQUEST_CODE);
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE}, READ_WRITE_EXT_STORAGE_PERMISSION_REQUEST);
                    } else {
                        SnackbarUtils.showTextLong(rootViewGroup, R.string.main_exempt_feature_permission_requirement);
                    }
                }
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
