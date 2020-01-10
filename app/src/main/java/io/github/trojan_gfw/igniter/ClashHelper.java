package io.github.trojan_gfw.igniter;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class ClashHelper {
    /*
     * Generate Clash running configuration file according to the template file
     */
    public static void ChangeClashConfig(String clashTemplateConfigPath, String clashConfigPath, long trojanPort, long clashSocksPort) {
        File templateFile = new File(clashTemplateConfigPath);
        File clashConfigFile = new File(clashConfigPath);
        if (templateFile.exists()) {
            try {
                String str;
                try (FileInputStream fis = new FileInputStream(templateFile)) {
                    byte[] content = new byte[(int) templateFile.length()];
                    fis.read(content);
                    str = new String(content);
                }

                str = str.replaceAll("__CLASH_SOCKS_PORT__", String.valueOf(clashSocksPort));
                str = str.replaceAll("__TROJAN_PORT__", String.valueOf(trojanPort));

                try (FileOutputStream fos = new FileOutputStream(clashConfigFile)) {
                    fos.write(str.getBytes());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void ShowConfig(String clashConfigPath) {
        File file = new File(clashConfigPath);

        try {
            try (FileInputStream fis = new FileInputStream(file)) {
                StringBuilder sb = new StringBuilder();
                byte[] content = new byte[(int) file.length()];
                fis.read(content);
                sb.append("\r\n");
                sb.append(new String(content));
                Log.i("ClashConfig", sb.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
