package io.github.trojan_gfw.igniter;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

public class TrojanConfig implements Parcelable {

    private String localAddr;
    private int localPort;
    private String remoteAddr;
    private String remoteServerName;
    private int remotePort;
    private String password;
    private boolean verifyCert;
    private String caCertPath;
    private boolean enableIpv6;
    private String cipherList;
    private String tls13CipherList;


    public TrojanConfig() {
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

    protected TrojanConfig(Parcel in) {
        localAddr = in.readString();
        localPort = in.readInt();
        remoteServerName = in.readString();
        remoteAddr = in.readString();
        remotePort = in.readInt();
        password = in.readString();
        verifyCert = in.readByte() != 0;
        caCertPath = in.readString();
        enableIpv6 = in.readByte() != 0;
        cipherList = in.readString();
        tls13CipherList = in.readString();
    }

    public static final Creator<TrojanConfig> CREATOR = new Creator<TrojanConfig>() {
        @Override
        public TrojanConfig createFromParcel(Parcel in) {
            return new TrojanConfig(in);
        }

        @Override
        public TrojanConfig[] newArray(int size) {
            return new TrojanConfig[size];
        }
    };

    public String generateTrojanConfigJSON() {
        try {
            return new JSONObject()
                    .put("local_addr", this.localAddr)
                    .put("local_port", this.localPort)
                    .put("remote_server_name", this.remoteServerName)
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
                    .setRemoteServerName(json.optString("remote_server_name"))
                    .setRemoteAddr(json.getString("remote_addr"))
                    .setRemotePort(json.getInt("remote_port"))
                    .setPassword(json.getJSONArray("password").getString(0))
                    .setEnableIpv6(json.getBoolean("enable_ipv6"))
                    .setVerifyCert(json.getJSONObject("ssl").getBoolean("verify"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void copyFrom(TrojanConfig that) {
        this
                .setLocalAddr(that.localAddr)
                .setLocalPort(that.localPort)
                .setRemoteServerName(that.remoteServerName)
                .setRemoteAddr(that.remoteAddr)
                .setRemotePort(that.remotePort)
                .setPassword(that.password)
                .setEnableIpv6(that.enableIpv6)
                .setVerifyCert(that.verifyCert)
                .setCaCertPath(that.caCertPath)
                .setCipherList(that.cipherList)
                .setTls13CipherList(that.tls13CipherList);

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

    public String getRemoteServerName() {
        return remoteServerName;
    }

    public TrojanConfig setRemoteServerName(String remoteServerName) {
        this.remoteServerName = remoteServerName;
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

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof TrojanConfig)) {
            return false;
        }
        TrojanConfig that = (TrojanConfig) obj;
        return (paramEquals(remoteServerName, that.remoteServerName) &&
                paramEquals(remoteAddr, that.remoteAddr) && paramEquals(remotePort, that.remotePort)
                && paramEquals(localAddr, that.localAddr) && paramEquals(localPort, that.localPort))
                && paramEquals(password, that.password) && paramEquals(verifyCert, that.verifyCert)
                && paramEquals(caCertPath, that.caCertPath) && paramEquals(enableIpv6, that.enableIpv6)
                && paramEquals(cipherList, that.cipherList) && paramEquals(tls13CipherList, that.tls13CipherList);
    }

    private static boolean paramEquals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(localAddr);
        dest.writeInt(localPort);
        dest.writeString(remoteServerName);
        dest.writeString(remoteAddr);
        dest.writeInt(remotePort);
        dest.writeString(password);
        dest.writeByte((byte) (verifyCert ? 1 : 0));
        dest.writeString(caCertPath);
        dest.writeByte((byte) (enableIpv6 ? 1 : 0));
        dest.writeString(cipherList);
        dest.writeString(tls13CipherList);
    }
}
