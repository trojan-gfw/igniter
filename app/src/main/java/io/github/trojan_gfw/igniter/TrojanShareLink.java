package io.github.trojan_gfw.igniter;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class TrojanShareLink {
    public static String GenerateShareLink(String remoteAddress, String remotePort, String password) {
        try {
            return "trojan://" + URLEncoder.encode(password, "UTF-8") + "@" + remoteAddress + ":" + remotePort;
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    public static String[] ConvertShareToTrojanConf(String trojanShareLink) {
        if (trojanShareLink.startsWith("trojan://")) {
            String[] tmp = new String[3];
            String tsl = trojanShareLink.substring(9);
            String[] temp = tsl.split(":");
            tmp[1] = temp[temp.length - 1].split("#")[0];
            try {
                Integer.parseInt(tmp[1]);
                temp[temp.length - 1] = "";
            } catch (Exception ex) {
                return null;
            }

            tsl = CombineToString(temp);
            String[] temp_1 = tsl.split("@");
            tmp[0] = temp_1[temp_1.length - 1];
            temp_1[temp_1.length - 1] = "";
            try {
                tmp[2] = URLDecoder.decode(CombineToString(temp_1), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return null;
            }
            return tmp;
        } else
            return null;
    }

    public static String CombineToString(String[] str) {
        String tmp = "";
        for (String s : str) {
            tmp += s;
        }
        return tmp;
    }
}
