package io.github.trojan_gfw.igniter;

import android.content.Intent;
import android.net.VpnService;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
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
                            .put("cert", getCacheDir() + "/cacert.pem")
                            .put("cipher", "ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:ECDHE-RSA-AES128-SHA:ECDHE-RSA-AES256-SHA:RSA-AES128-GCM-SHA256:RSA-AES256-GCM-SHA384:RSA-AES128-SHA:RSA-AES256-SHA:RSA-3DES-EDE-SHA")
                            .put("alpn", new JSONArray().put("h2").put("http/1.1")))
                    .put("enable_ipv6", enableIpv6)
                    .toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

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
        startStopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                TrojanService serviceInstance = TrojanService.getInstance();
                if (serviceInstance == null) {
                    String config = getConfig(remoteAddrText.getText().toString(),
                            Integer.parseInt(remotePortText.getText().toString()),
                            passwordText.getText().toString(),
                            ipv6Switch.isChecked(),
                            verifySwitch.isChecked());
                    File file = new File(getFilesDir(), "config.json");
                    try {
                        try (FileOutputStream fos = new FileOutputStream(file)) {
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
                    serviceInstance.stop();
                }
            }
        });
        File file = new File(getFilesDir(), "config.json");
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
        file = new File(getCacheDir(), "cacert.pem");
        if (!file.exists()) {
            try {
                try (InputStream is = getResources().openRawResource(R.raw.cacert);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            startService(new Intent(this, TrojanService.class));
        }
    }
}
