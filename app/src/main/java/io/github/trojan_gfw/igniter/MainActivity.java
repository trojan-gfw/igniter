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
import java.io.FileOutputStream;
import android.app.Activity;

public class MainActivity extends AppCompatActivity {

    EditText remoteAddrText;
    EditText remotePortText;
    EditText passwordText;
    Button startStopButton;

    private static String getConfig(String remoteAddr, short remotePort, String password) {
        try {
            return new JSONObject()
                    .put("local_addr", "127.0.0.1")
                    .put("local_port", 1080)
                    .put("remote_addr", remoteAddr)
                    .put("remote_port", remotePort)
                    .put("password", new JSONArray()
                            .put(password))
                    .put("log_level", 0)
                    .put("ssl", new JSONObject()
                            .put("verify", false) // TODO: Very bad idea. Remove after including CA certificates.
                            .put("cipher", "ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305-SHA256:ECDHE-RSA-CHACHA20-POLY1305-SHA256:ECDHE-RSA-AES128-SHA:ECDHE-RSA-AES256-SHA:RSA-AES128-GCM-SHA256:RSA-AES256-GCM-SHA384:RSA-AES128-SHA:RSA-AES256-SHA:RSA-3DES-EDE-SHA")
                            .put("alpn", new JSONArray().put("h2").put("http/1.1")))
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
        startStopButton = findViewById(R.id.startStopButton);
        startStopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String config = getConfig(remoteAddrText.getText().toString(),
                        Short.parseShort(remotePortText.getText().toString()),
                        passwordText.getText().toString());
                File file = new File(getCacheDir(), "config.json");
                try {
                    FileOutputStream os = new FileOutputStream(file);
                    os.write(config.getBytes());
                    os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Intent i = VpnService.prepare(getApplicationContext());
                if (i != null) {
                    startActivityForResult(i, 0);
                } else {
                    onActivityResult(0, Activity.RESULT_OK, null);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            startService(new Intent(this, TrojanService.class));
        }
    }
}
