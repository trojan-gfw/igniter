package io.github.trojan_gfw.igniter;

public class AppConfig {
    public static final String SERVER_CONFIG_FILE = "config.json";
    public static final String CA_CERT_FILE = "cacert.pem";

    private static AppConfig instance = null;
    private String cipher = "ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:" +
            "ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:" +
            "ECDHE-RSA-CHACHA20-POLY1305:ECDHE-RSA-AES128-SHA:ECDHE-RSA-AES256-SHA:RSA-AES128-GCM-SHA256:" +
            "RSA-AES256-GCM-SHA384:RSA-AES128-SHA:RSA-AES256-SHA:RSA-3DES-EDE-SHA";
    private Boolean enableIPv6 = false;

    public static AppConfig getInstance() {
        if (instance == null)
            instance = new AppConfig();

        return instance;
    }

    public Boolean getEnableIPv6() {
        return enableIPv6;
    }

    public void setEnableIPv6(Boolean val) {
        enableIPv6 = val;
    }

    public String getCipher() {
        return cipher;
    }
}
