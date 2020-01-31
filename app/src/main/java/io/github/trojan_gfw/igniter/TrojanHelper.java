package io.github.trojan_gfw.igniter;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class TrojanHelper {
    private static final String TAG = "TrojanConfig";

    public static void WriteTrojanConfig(String remoteAddr, int remotePort, String password,
                                         boolean enableIpv6, boolean verify, String caCertPath, String trojanConfigPath) {
        String config = generateTrojanConfigJSON(remoteAddr,
                remotePort,
                password,
                enableIpv6,
                verify, caCertPath);
        File file = new File(trojanConfigPath);
        try {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(config.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String generateTrojanConfigJSON(String remoteAddr, int remotePort, String password,
                                                   boolean enableIpv6, boolean verify, String caCertPath) {
        try {
            return new JSONObject()
                    .put("local_addr", "127.0.0.1")
                    .put("local_port", 1081)
                    .put("remote_addr", remoteAddr)
                    .put("remote_port", remotePort)
                    .put("password", new JSONArray().put(password))
                    .put("log_level", 2) // WARN
                    .put("ssl", new JSONObject()
                            .put("verify", verify)
                            .put("cert", caCertPath)
                            .put("cipher", "ECDHE-ECDSA-AES128-GCM-SHA256:"
                                    + "ECDHE-RSA-AES128-GCM-SHA256:"
                                    + "ECDHE-ECDSA-CHACHA20-POLY1305:"
                                    + "ECDHE-RSA-CHACHA20-POLY1305:"
                                    + "ECDHE-ECDSA-AES256-GCM-SHA384:"
                                    + "ECDHE-RSA-AES256-GCM-SHA384:"
                                    + "ECDHE-ECDSA-AES256-SHA:"
                                    + "ECDHE-ECDSA-AES128-SHA:"
                                    + "ECDHE-RSA-AES128-SHA:"
                                    + "ECDHE-RSA-AES256-SHA:"
                                    + "DHE-RSA-AES128-SHA:"
                                    + "DHE-RSA-AES256-SHA:"
                                    + "AES128-SHA:"
                                    + "AES256-SHA:"
                                    + "DES-CBC3-SHA")
                            .put("cipher_tls13", "TLS_AES_128_GCM_SHA256:"
                                    + "TLS_CHACHA20_POLY1305_SHA256:"
                                    + "TLS_AES_256_GCM_SHA384")
                            .put("alpn", new JSONArray().put("h2").put("http/1.1")))
                    .put("enable_ipv6", enableIpv6)
                    .toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void ChangeListenPort(String trojanConfigPath, long port) {
        File file = new File(trojanConfigPath);
        if (file.exists()) {
            try {
                String str;
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] content = new byte[(int) file.length()];
                    fis.read(content);
                    str = new String(content);

                }
                JSONObject json = new JSONObject(str);
                json.put("local_port", port);
                try (FileOutputStream fos = new FileOutputStream(file)) {

                    fos.write(json.toString().getBytes());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void ShowConfig(String trojanConfigPath) {
        File file = new File(trojanConfigPath);

        try {
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] content = new byte[(int) file.length()];
                fis.read(content);
                Log.i(TAG, new String(content));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
