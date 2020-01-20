package io.github.trojan_gfw.igniter;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.net.VpnService;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.*;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {

    private static final int VPN_REQUEST_CODE = 0;

    private static boolean onBusy;

    private ImageView logoImage;
    private EditText remoteAddrText;
    private EditText remotePortText;
    private EditText passwordText;
    private EditText shareLinkText;
    private Switch ipv6Switch;
    private Switch verifySwitch;
    private Switch clashSwitch;
    private TextView clashLink;
    private Button startStopButton;

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
        passwordText.setEnabled(inputEnabled);
        shareLinkText.setEnabled(inputEnabled);
        ipv6Switch.setEnabled(inputEnabled);
        verifySwitch.setEnabled(inputEnabled);
        clashSwitch.setEnabled(inputEnabled);
        clashLink.setEnabled(inputEnabled);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logoImage = findViewById(R.id.imageView);
        remoteAddrText = findViewById(R.id.remoteAddrText);
        remotePortText = findViewById(R.id.remotePortText);
        passwordText = findViewById(R.id.passwordText);
        shareLinkText = findViewById(R.id.shareLinkText);
        ipv6Switch = findViewById(R.id.ipv6Switch);
        verifySwitch = findViewById(R.id.verifySwitch);
        clashSwitch = findViewById(R.id.clashSwitch);
        clashLink = findViewById(R.id.clashLink);
        clashLink.setMovementMethod(LinkMovementMethod.getInstance());
        startStopButton = findViewById(R.id.startStopButton);

        Constants.Init(this);

        copyRawResourceToDir(R.raw.cacert, Constants.getCaCertPath(), true);
        copyRawResourceToDir(R.raw.country, Constants.getCountryMmdbPath(), true);
        // copy clash template configuration
        copyRawResourceToDir(R.raw.clash_config, Constants.getClashTemplatePath(), true);

        final TrojanShareLink trojanShareLink = new TrojanShareLink();

        shareLinkText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                onBusy = true;
                String[] shareLink = trojanShareLink.ConvertShareToTrojanConf(shareLinkText.getText().toString());
                if (shareLink != null && shareLink.length > 0) {
                    remoteAddrText.setText(shareLink[0]);
                    remoteAddrText.setSelection(remoteAddrText.getText().length());
                    remotePortText.setText(shareLink[1]);
                    remotePortText.setSelection(remotePortText.getText().length());
                    passwordText.setText(shareLink[2]);
                    passwordText.setSelection(passwordText.getText().length());

                }

                onBusy = false;
            }
        });

        TextWatcher trojanTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!onBusy)
                    shareLinkText.setText(trojanShareLink.GenerateShareLink(remoteAddrText.getText().toString(), remotePortText.getText().toString(), passwordText.getText().toString()));
                shareLinkText.setSelection(shareLinkText.getText().length());

            }
        };

        remoteAddrText.addTextChangedListener(trojanTextWatcher);

        remotePortText.addTextChangedListener(trojanTextWatcher);

        passwordText.addTextChangedListener(trojanTextWatcher);

        shareLinkText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                shareLinkText.selectAll();
                return false;
            }
        });

        startStopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!remoteAddrText.getText().toString().isEmpty() && !remotePortText.getText().toString().isEmpty() && !passwordText.getText().toString().isEmpty()) {
                    ProxyService serviceInstance = ProxyService.getInstance();
                    if (serviceInstance == null) {
                        TrojanHelper.WriteTrojanConfig(
                                remoteAddrText.getText().toString(),
                                Integer.parseInt(remotePortText.getText().toString()),
                                passwordText.getText().toString(),
                                ipv6Switch.isChecked(),
                                verifySwitch.isChecked(),
                                Constants.getCaCertPath(),
                                Constants.getTrojanConfigPath()
                        );
                        TrojanHelper.ShowConfig(Constants.getTrojanConfigPath());

                        Intent i = VpnService.prepare(getApplicationContext());
                        if (i != null) {
                            startActivityForResult(i, VPN_REQUEST_CODE);
                        } else {
                            onActivityResult(VPN_REQUEST_CODE, Activity.RESULT_OK, null);
                        }
                    } else {
                        serviceInstance.stop();
                    }
                } else {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Alert")
                            .setMessage("Please enter valid Trojan config.")
                            .setPositiveButton("OK", null)
                            .show();
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
        File file = new File(Constants.getTrojanConfigPath());
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
