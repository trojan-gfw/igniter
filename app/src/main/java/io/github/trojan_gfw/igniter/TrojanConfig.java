package io.github.trojan_gfw.igniter;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public class TrojanConfig {

    private String localAddr;
    private int localPort;
    private String remoteAddr;
    private int remotePort;
    private String password;
    private boolean verifyCert;
    private String caCertPath;
    private boolean enableIpv6;
    private String cipherList;
    private String tls13CipherList;


    TrojanConfig() {
        // defaults
        this.localAddr = "127.0.0.1";
        this.localPort = 1081;
        this.remotePort = 443;
        this.verifyCert = true;
        this.cipherList = "ECDHE-ECDSA-AES128-GCM-SHA256:"
                + "ECDHE-RSA-AES128-GCM-SHA256:"
                + "ECDHE-ECDSA-CHACHA20-POLY1305:"
                + "ECDHE-RSA-CHACHA20-POLY1305:"
                + "ECDHE-ECDSA-AES256-GCM-SHA384:"
                + "ECDHE-RSA-AES256-GCM-SHA384:"
                + "ECDHE-ECDSA-AES256-SHA:"
                + "ECDHE-ECDSA-AES128-SHA:"
                + "ECDHE-RSA-AES128-SHA:"
                + "ECDHE-RSA-AES256-SHA:"
                + "DHE-RSA-AES128-SHA:"
                + "DHE-RSA-AES256-SHA:"
                + "AES128-SHA:"
                + "AES256-SHA:"
                + "DES-CBC3-SHA";
        this.tls13CipherList = "TLS_AES_128_GCM_SHA256:"
                + "TLS_CHACHA20_POLY1305_SHA256:"
                + "TLS_AES_256_GCM_SHA384";
    }

    public String generateTrojanConfigJSON() {
        try {
            return new JSONObject()
                    .put("local_addr", this.localAddr)
                    .put("local_port", this.localPort)
                    .put("remote_addr", this.remoteAddr)
                    .put("remote_port", this.remotePort)
                    .put("password", new JSONArray().put(password))
                    .put("log_level", 2) // WARN
                    .put("ssl", new JSONObject()
                            .put("verify", this.verifyCert)
                            .put("cert", this.caCertPath)
                            .put("cipher", this.cipherList)
                            .put("cipher_tls13", this.tls13CipherList)
                            .put("alpn", new JSONArray().put("h2").put("http/1.1")))
                    .put("enable_ipv6", this.enableIpv6)
                    .toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void fromJSON(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            this.setLocalAddr(json.getString("local_addr"))
                    .setLocalPort(json.getInt("local_port"))
                    .setRemoteAddr(json.getString("remote_addr"))
                    .setRemotePort(json.getInt("remote_port"))
                    .setPassword(json.getJSONArray("password").getString(0))
                    .setEnableIpv6(json.getBoolean("enable_ipv6"))
                    .setVerifyCert(json.getJSONObject("ssl").getBoolean("verify"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isValidRunningConfig() {
        return !TextUtils.isEmpty(this.caCertPath)
                && !TextUtils.isEmpty(this.remoteAddr)
                && !TextUtils.isEmpty(this.password);
    }

    public String getLocalAddr() {
        return localAddr;
    }

    public TrojanConfig setLocalAddr(String localAddr) {
        this.localAddr = localAddr;
        return this;
    }

    public int getLocalPort() {
        return localPort;
    }

    public TrojanConfig setLocalPort(int localPort) {
        this.localPort = localPort;
        return this;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public TrojanConfig setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
        return this;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public TrojanConfig setRemotePort(int remotePort) {
        this.remotePort = remotePort;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public TrojanConfig setPassword(String password) {
        this.password = password;
        return this;
    }

    public boolean getVerifyCert() {
        return verifyCert;
    }

    public TrojanConfig setVerifyCert(boolean verifyCert) {
        this.verifyCert = verifyCert;
        return this;
    }

    public String getCaCertPath() {
        return caCertPath;
    }

    public TrojanConfig setCaCertPath(String caCertPath) {
        this.caCertPath = caCertPath;
        return this;
    }

    public boolean getEnableIpv6() {
        return enableIpv6;
    }

    public TrojanConfig setEnableIpv6(boolean enableIpv6) {
        this.enableIpv6 = enableIpv6;
        return this;
    }

    public String getCipherList() {
        return cipherList;
    }

    public TrojanConfig setCipherList(String cipherList) {
        this.cipherList = cipherList;
        return this;
    }

    public String getTls13CipherList() {
        return tls13CipherList;
    }

    public TrojanConfig setTls13CipherList(String tls13CipherList) {
        this.tls13CipherList = tls13CipherList;
        return this;
    }
}
