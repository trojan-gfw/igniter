package io.github.trojan_gfw.igniter;

import java.net.URI;

public class TrojanURLHelper {
    public static String GenerateTrojanURL(TrojanConfig trojanConfig) {
        return null;
    }

    public static TrojanConfig ParseTrojanURL(String trojanURLStr) {
        URI trojanUri;
        try {
            trojanUri = new URI(trojanURLStr);
        } catch (java.net.URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
        String scheme = trojanUri.getScheme();
        if (scheme == null) {
            return null;
        }
        if (!scheme.equals("trojan"))
            return null;
        String host = trojanUri.getHost();
        int port = trojanUri.getPort();
        String userInfo = trojanUri.getUserInfo();

        TrojanConfig retConfig = new TrojanConfig();
        return retConfig;
    }
}
