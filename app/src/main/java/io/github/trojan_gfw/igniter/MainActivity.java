package io.github.trojan_gfw.igniter;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {

    private static final int VPN_REQUEST_CODE = 0;
    private static final String CONNECTION_TEST_URL = "https://www.google.com";

    private EditText remoteAddrText;
    private EditText remotePortText;
    private EditText passwordText;
    private Switch ipv6Switch;
    private Switch verifySwitch;
    private Switch clashSwitch;
    private TextView clashLink;
    private Button startStopButton;
    private EditText trojanURLText;
    protected Button testConnectionButton;

    private BroadcastReceiver serviceStateReceiver;

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
        testConnectionButton = findViewById(R.id.testConnectionButton);

        Globals.Init(this);

        copyRawResourceToDir(R.raw.cacert, Globals.getCaCertPath(), true);
        copyRawResourceToDir(R.raw.country, Globals.getCountryMmdbPath(), true);
        copyRawResourceToDir(R.raw.clash_config, Globals.getClashConfigPath(), false);

        remoteAddrText.addTextChangedListener(new TextViewListener() {
            @Override
            protected void onTextChanged(String before, String old, String aNew, String after) {
                // update TextView
                startUpdates(); // to prevent infinite loop.
                TrojanConfig ins = Globals.getTrojanConfigInstance();
                ins.setRemoteAddr(remoteAddrText.getText().toString());
                endUpdates();
            }
        });

        remotePortText.addTextChangedListener(new TextViewListener() {
            @Override
            protected void onTextChanged(String before, String old, String aNew, String after) {
                // update TextView
                startUpdates(); // to prevent infinite loop.
                TrojanConfig ins = Globals.getTrojanConfigInstance();
                String portStr = remotePortText.getText().toString();
                try {
                    int port = Integer.parseInt(portStr);
                    ins.setRemotePort(port);
                } catch (NumberFormatException e) {
                    // Ignore when we get invalid number
                    e.printStackTrace();
                }
                endUpdates();
            }
        });

        passwordText.addTextChangedListener(new TextViewListener() {
            @Override
            protected void onTextChanged(String before, String old, String aNew, String after) {
                // update TextView
                startUpdates(); // to prevent infinite loop.
                TrojanConfig ins = Globals.getTrojanConfigInstance();
                ins.setPassword(passwordText.getText().toString());
                endUpdates();
            }
        });


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

        testConnectionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                new TestConnection(MainActivity.this).execute(CONNECTION_TEST_URL);
            }
        });

        serviceStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(ProxyService.STATUS_EXTRA_NAME, ProxyService.STARTED);
                updateViews(state);
            }
        };

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Intent intent = new Intent(this, ProxyService.class);
            intent.putExtra(ProxyService.CLASH_EXTRA_NAME, clashSwitch.isChecked());
            startService(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        File file = new File(Globals.getTrojanConfigPath());
        if (file.exists()) {
            try {
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] content = new byte[(int) file.length()];
                    fis.read(content);
                    JSONObject json = new JSONObject(new String(content));
                    remoteAddrText.setText(json.getString("remote_addr"));
                    remotePortText.setText(String.valueOf(json.getInt("remote_port")));
                    passwordText.setText(json.getJSONArray("password").getString(0));
                    ipv6Switch.setChecked(json.getBoolean("enable_ipv6"));
                    verifySwitch.setChecked(json.getJSONObject("ssl").getBoolean("verify"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

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
