package io.github.trojan_gfw.igniter;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

public class TrojanConfig implements Parcelable {

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    private String config;

    public TrojanConfig() {
        try {
            JSONObject jsonObject = new JSONObject("{\"run_type\": \"client\",\"local_addr\": \"127.0.0.1\",\"local_port\": 1080,\"remote_addr\": \"your_server\",\"remote_port\": 443,\"password\": [\"your_password\"],\"ssl\": {\"sni\": \"your_domain_name\"},\"mux\": {\"enabled\": true,\"concurrency\": 8,\"idle_timeout\": 60},\"websocket\": {\"enabled\": false,\"path\": \"/path\",\"double_tls\": true,\"obfuscation_password\": \"\"}}");
            config = jsonObject.toString(4);
        } catch (Throwable t) {

        }
    }

    public String name() {
        try {
            JSONObject jsonObject = new JSONObject(config);
            String addr = jsonObject.getString("remote_addr");
            int port = jsonObject.getInt("remote_port");
            return addr + ":" + String.valueOf(port);
        }catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    protected TrojanConfig(Parcel in) {
        config = in.readString();
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
            JSONObject jsonObject = new JSONObject(config);
            return jsonObject.toString(4);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void fromJSON(String jsonStr) {
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            config = jsonObject.toString(4);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void copyFrom(TrojanConfig that) {
        config = that.config;
    }

    public boolean isValidRunningConfig() {
        return !name().equals("");
    }


    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof TrojanConfig)) {
            return false;
        }
        TrojanConfig that = (TrojanConfig) obj;
        return that.config == this.config;
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
        dest.writeString(config);
    }
}
