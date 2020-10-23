package io.github.trojan_gfw.igniter;

import com.google.gson.Gson;

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
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return trojanUri.toASCIIString();
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

    /**
     * Merge Trojan URL parse result and Trojan config instance
     *
     * @param srcParseResult parsed Trojan URL
     * @param srcConfig      the source Trojan config instance
     * @return A deep clone of the source Trojan config instance
     */
    public static TrojanConfig CombineTrojanURLParseResultToTrojanConfig(TrojanURLParseResult srcParseResult,
                                                                         TrojanConfig srcConfig) {
        if (srcConfig == null || srcParseResult == null) {
            return null;
        }
        Gson gson = new Gson();

        TrojanConfig dstConfig = gson.fromJson(gson.toJson(srcConfig), TrojanConfig.class);
        TrojanURLParseResult dstParseResult = gson.fromJson(gson.toJson(srcParseResult), TrojanURLParseResult.class);
        dstConfig.setRemoteAddr(dstParseResult.host);
        dstConfig.setRemotePort(dstParseResult.port);
        dstConfig.setPassword(dstParseResult.password);
        return dstConfig;
    }
}
