package io.github.trojan_gfw.igniter;

import java.net.URI;

public class TrojanURLHelper {
    public static String GenerateTrojanURL(TrojanConfig trojanConfig) {

        URI trojanUri;
        try {
            trojanUri = new URI("trojan",
                    trojanConfig.getPassword(),
                    trojanConfig.getRemoteAddr(),
                    trojanConfig.getRemotePort(),
                    null, null, null);
        } catch (java.net.URISyntaxException e) {
            e.printStackTrace();
            return null;
        }

        return trojanUri.toString();
    }

    public static TrojanURLParseResult ParseTrojanURL(String trojanURLStr) {
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

        TrojanURLParseResult retConfig = new TrojanURLParseResult();
        retConfig.host = host;
        retConfig.port = port;
        retConfig.password = userInfo;
        return retConfig;
    }

    public static TrojanConfig CombineTrojanURLParseResultToTrojanConfig(TrojanURLParseResult result,
                                                                         TrojanConfig config) {
        if (config == null || result == null) {
            return null;
        }
        config.setRemoteAddr(result.host);
        config.setRemotePort(result.port);
        config.setPassword(result.password);
        return config;
    }
}
