package io.github.trojan_gfw.igniter;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class TrojanHelper {
    private static final String TAG = "TrojanConfig";

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
                LogHelper.i(TAG, new String(content));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
