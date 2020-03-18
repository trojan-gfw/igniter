package io.github.trojan_gfw.igniter;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrojanHelper {
    private static final String SINGLE_CONFIG_TAG = "TrojanConfig";
    private static final String CONFIG_LIST_TAG = "TrojanConfigList";

    public static boolean writeTrojanServerConfigList(List<TrojanConfig> configList, String trojanConfigListPath) {
        JSONArray jsonArray = new JSONArray();
        for (TrojanConfig config : configList) {
            try {
                JSONObject jsonObject = new JSONObject(config.generateTrojanConfigJSON());
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
        }
        String configStr = jsonArray.toString();
        File file = new File(trojanConfigListPath);
        if (file.exists()) {
            file.delete();
        }
        try (OutputStream fos = new FileOutputStream(file)) {
            fos.write(configStr.getBytes());
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @NonNull
    public static List<TrojanConfig> readTrojanServerConfigList(String trojanConfigListPath) {
        File file = new File(trojanConfigListPath);
        if (!file.exists()) {
            return Collections.emptyList();
        }
        try (InputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            String json = new String(data);
            JSONArray jsonArr = new JSONArray(json);
            int len = jsonArr.length();
            List<TrojanConfig> list = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                list.add(parseTrojanConfigFromJSON(jsonArr.getJSONObject(i).toString()));
            }
            return list;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public static void ShowTrojanConfigList(String trojanConfigListPath) {
        File file = new File(trojanConfigListPath);

        try {
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] content = new byte[(int) file.length()];
                fis.read(content);
                LogHelper.i(CONFIG_LIST_TAG, new String(content));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String parseTrojanConfigToJSON(TrojanConfig config) {
        try {
            /*JSONObject json = new JSONObject();
            json.put("local_addr", config.getLocalAddr());
            json.put("local_port", config.getLocalPort());
            json.put("remote_addr", config.getRemoteAddr());
            json.put("remote_port", config.getRemotePort());
            json.put("password", config.getPassword());
            json.put("verify_cert", config.getVerifyCert());
            json.put("ca_cert_path", config.getCaCertPath());
            json.put("enable_ipv6", config.getEnableIpv6());
            json.put("cipher_list", config.getCipherList());
            json.put("tls13_cipher_list", config.getTls13CipherList());
            return json.toString();*/
            return config.generateTrojanConfigJSON();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static TrojanConfig parseTrojanConfigFromJSON(String json) {
        TrojanConfig config = new TrojanConfig();
        config.fromJSON(json);
        return config;
    }

    public static void WriteTrojanConfig(TrojanConfig trojanConfig, String trojanConfigPath) {
        String config = trojanConfig.generateTrojanConfigJSON();
        File file = new File(trojanConfigPath);
        try {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(config.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                LogHelper.i(SINGLE_CONFIG_TAG, new String(content));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
