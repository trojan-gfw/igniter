package io.github.trojan_gfw.igniter;


import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
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

import io.github.trojan_gfw.igniter.common.os.Task;
import io.github.trojan_gfw.igniter.common.os.Threads;
import io.github.trojan_gfw.igniter.common.utils.SnackbarUtils;
import io.github.trojan_gfw.igniter.exempt.activity.ExemptAppActivity;
import io.github.trojan_gfw.igniter.servers.activity.ServerListActivity;
import io.github.trojan_gfw.igniter.servers.data.ServerListDataManager;
import io.github.trojan_gfw.igniter.servers.data.ServerListDataSource;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int SERVER_LIST_CHOOSE_REQUEST_CODE = 1024;
    private static final int EXEMPT_APP_CONFIGURE_REQUEST_CODE = 2077;
    private static final int VPN_REQUEST_CODE = 0;
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

    private BroadcastReceiver serviceStateReceiver;
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

    private void createNotificationChannel(String channelId) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notification_channel_name);
            String description = getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void destoryNotificationChannel(String channelId) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.deleteNotificationChannel(channelId);
        }
    }

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

        Globals.Init(this);
        createNotificationChannel(getString(R.string.notification_channel_id));

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

                ProxyService serviceInstance = ProxyService.getInstance();
                if (serviceInstance == null) {
                    TrojanHelper.WriteTrojanConfig(
                            Globals.getTrojanConfigInstance(),
                            Globals.getTrojanConfigPath()
                    );
                    TrojanHelper.ShowConfig(Globals.getTrojanConfigPath());

                    Intent i = VpnService.prepare(getApplicationContext());
                    if (i != null) {
                        startActivityForResult(i, VPN_REQUEST_CODE);
                    } else {
                        onActivityResult(VPN_REQUEST_CODE, Activity.RESULT_OK, null);
                    }
                } else {
                    serviceInstance.stop();
                }

            }
        });

        serviceStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(ProxyService.STATUS_EXTRA_NAME, ProxyService.STARTED);
                updateViews(state);
            }
        };

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
                        serverListDataManager.saveServerConfig(Globals.getTrojanConfigInstance());
                        showSaveConfigResult(true);
                    }
                });
            }
        });
        serverListDataManager = new ServerListDataManager(Globals.getTrojanConfigListPath());
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
        if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Intent intent = new Intent(this, ProxyService.class);
            intent.putExtra(ProxyService.CLASH_EXTRA_NAME, clashSwitch.isChecked());
            startService(intent);
        } else if (SERVER_LIST_CHOOSE_REQUEST_CODE == requestCode && resultCode == Activity.RESULT_OK && data != null) {
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
            if (isProxyRunning()) {
                SnackbarUtils.showTextLong(rootViewGroup, R.string.main_restart_proxy_service_tip);
            }
        }
    }

    private boolean isProxyRunning() {
        return ProxyService.getInstance() != null;
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
                new TestConnection(MainActivity.this).execute(CONNECTION_TEST_URL);
                return true;
            case R.id.action_show_develop_info_logcat:
                util.Util.logGoRoutineCount();
                util.Util.logGoroutineStackTrace();
                return true;
            case R.id.action_view_server_list:
                clearEditTextFocus();
                startActivityForResult(ServerListActivity.create(MainActivity.this), SERVER_LIST_CHOOSE_REQUEST_CODE);
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

                    remoteAddrText.setText(ins.getRemoteAddr());
                    remotePortText.setText(String.valueOf(ins.getRemotePort()));
                    passwordText.setText(ins.getPassword());
                    ipv6Switch.setChecked(ins.getEnableIpv6());
                    verifySwitch.setChecked(ins.getVerifyCert());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ProxyService serviceInstance = ProxyService.getInstance();
        if (serviceInstance == null) {
            updateViews(ProxyService.STOPPED);
        } else {
            updateViews(serviceInstance.getState());
            clashSwitch.setChecked(serviceInstance.enable_clash);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                serviceStateReceiver, new IntentFilter(getString(R.string.bc_service_state))
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceStateReceiver);
    }
}
