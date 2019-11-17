package io.github.trojan_gfw.igniter;

import android.content.Intent;
import android.net.VpnService;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.app.Activity;
import android.widget.Switch;

public class MainActivity extends AppCompatActivity {

    private static final int VPN_REQUEST_CODE = 0;

    private EditText remoteAddrText;
    private EditText remotePortText;
    private EditText passwordText;
    private Switch ipv6Switch;
    private Switch verifySwitch;
    private Button startStopButton;

    private AppConfig appConfig = AppConfig.getInstance();
    private View.OnClickListener ipv6SwitchListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            appConfig.setEnableIPv6(((Switch) v).isChecked());
//            Log.d("IPv6 Switch status: ", Boolean.toString(((Switch) v).isChecked()));
        }
    };
    private View.OnClickListener startStopButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TrojanService serviceInstance = TrojanService.getInstance();
            if (serviceInstance == null) {
                String config = getConfig(remoteAddrText.getText().toString(),
                        Integer.parseInt(remotePortText.getText().toString()),
                        passwordText.getText().toString(),
                        ipv6Switch.isChecked(),
                        verifySwitch.isChecked());
                File file = new File(String.format("%s/%s", getFilesDir(), AppConfig.SERVER_CONFIG_FILE));
                try {
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        if (config != null)
                            fos.write(config.getBytes());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Intent i = VpnService.prepare(getApplicationContext());
                if (i != null) {
                    startActivityForResult(i, VPN_REQUEST_CODE);
                } else {
                    onActivityResult(VPN_REQUEST_CODE, Activity.RESULT_OK, null);
                }

            } else {
                serviceInstance.stopService();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        remoteAddrText = findViewById(R.id.remoteAddrText);
        remotePortText = findViewById(R.id.remotePortText);
        passwordText = findViewById(R.id.passwordText);
        ipv6Switch = findViewById(R.id.ipv6Switch);
        verifySwitch = findViewById(R.id.verifySwitch);
        startStopButton = findViewById(R.id.startStopButton);

        ipv6Switch.setOnClickListener(ipv6SwitchListener);
        startStopButton.setOnClickListener(startStopButtonListener);

        // Read the configuration of the server to initialize the app.
        File file = new File(String.format("%s/%s", getFilesDir(), AppConfig.SERVER_CONFIG_FILE));
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

        // Write the certificates of all CAs to local storage.
//        file = new File(String.format("%s/%s", getCacheDir(), AppConfig.CA_CERT_FILE));
//        if (!file.exists()) {
//            try {
//                try (InputStream is = getResources().openRawResource(R.raw.cacert);
//                     FileOutputStream fos = new FileOutputStream(file)) {
//                     byte[] buf = new byte[1024];
//                     int len;
//                     while ((len = is.read(buf)) > 0) {
//                         fos.write(buf, 0, len);
//                     }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            startService(new Intent(this, TrojanService.class));
        }
    }

    private String getConfig(String remoteAddr, int remotePort, String password, boolean enableIpv6, boolean verify) {
        try {
            return new JSONObject()
                    .put("local_addr", "127.0.0.1")
                    .put("local_port", 1080)
                    .put("remote_addr", remoteAddr)
                    .put("remote_port", remotePort)
                    .put("password", new JSONArray()
                            .put(password))
                    .put("log_level", 2) // WARN
                    .put("ssl", new JSONObject()
                            .put("verify", verify)
                            .put("cert", String.format("%s/%s", getCacheDir(), AppConfig.CA_CERT_FILE))
                            .put("cipher", appConfig.getCipher())
                            .put("alpn", new JSONArray().put("h2").put("http/1.1")))
                    .put("enable_ipv6", enableIpv6)
                    .toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
