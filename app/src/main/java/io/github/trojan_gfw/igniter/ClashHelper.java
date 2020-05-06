package io.github.trojan_gfw.igniter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClashHelper {

    private static final String TAG = "ClashConfig";

    // In general, we capture only one group for replacement
    public static final Pattern clashSocksPortPattern = Pattern.compile("^socks.*port:\\s+(\\d+)", Pattern.MULTILINE);
    public static final Pattern trojanPortPattern = Pattern.compile("trojan.*socks.*port:\\s+(\\d+)", Pattern.MULTILINE);

    /*
     * Generate Clash running configuration file according to the template file
     */
    public static void ChangeClashConfig(String clashConfigPath, long trojanPort, long clashSocksPort) {
        File tmpClashConfigFile = new File(clashConfigPath + ".tmp");
        File clashConfigFile = new File(clashConfigPath);
        try {
            String str;
            try (FileInputStream fis = new FileInputStream(clashConfigFile)) {
                long origClashConfigLen = clashConfigFile.length();
                byte[] content = new byte[(int) origClashConfigLen];
                if (fis.read(content) != origClashConfigLen) {
                    LogHelper.e(TAG, "fail to read full content of clash config file");
                }
                str = new String(content);
            }

            String clashSocksPortStr = String.valueOf(clashSocksPort);
            String trojanPortStr = String.valueOf(trojanPort);
            str = replaceGroup(clashSocksPortPattern, str, 1, clashSocksPortStr);
            str = replaceGroup(trojanPortPattern, str, 1, trojanPortStr);

            try (FileOutputStream fos = new FileOutputStream(tmpClashConfigFile)) {
                fos.write(str.getBytes());
            }

            if (!clashConfigFile.delete()) {
                LogHelper.e(TAG, "fail to delete old clash config file");
            }
            if (!tmpClashConfigFile.renameTo(clashConfigFile)) {
                LogHelper.e(TAG, "fail to rename tmp clash config file");
            }

        } catch (Exception e) {
            e.printStackTrace();
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
                LogHelper.v(TAG, sb.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String replaceGroup(Pattern regex, String source,
                                      int groupToReplace, String replacement) throws Exception {
        return replaceGroup(regex, source, groupToReplace, 1, replacement);
    }

    public static String replaceGroup(Pattern regex, String source,
                                      int groupToReplace, int groupOccurrence, String replacement) throws Exception {
        Matcher m = regex.matcher(source);
        for (int i = 0; i < groupOccurrence; i++) {
            if (!m.find())
                throw new Exception("Pattern not found"); // pattern not met, may also throw an exception here
        }
        return new StringBuilder(source)
                .replace(m.start(groupToReplace), m.end(groupToReplace), replacement)
                .toString();
    }
}
