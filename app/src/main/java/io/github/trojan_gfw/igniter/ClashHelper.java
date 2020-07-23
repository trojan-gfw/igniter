package io.github.trojan_gfw.igniter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClashHelper {

    private static final String TAG = "ClashConfig";

    public static void ShowConfig(String clashConfigPath) {
        File file = new File(clashConfigPath);

        try {
            try (FileInputStream fis = new FileInputStream(file)) {
                StringBuilder sb = new StringBuilder();
                byte[] content = new byte[(int) file.length()];
                fis.read(content);
                sb.append("\r\n");
                sb.append(new String(content));
                LogHelper.v(TAG, sb.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
