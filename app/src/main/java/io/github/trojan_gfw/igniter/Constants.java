package io.github.trojan_gfw.igniter;

import android.content.Context;

public class Constants {

    private static String cacheDir;
    private static String filesDir;

    public static void Init(Context ctx) {
        cacheDir = ctx.getCacheDir().getAbsolutePath();
        filesDir = ctx.getFilesDir().getAbsolutePath();

    }

    public static String getCaCertPath() {
        return PathHelper.combine(cacheDir, "cacert.pem");
    }

    public static String getCountryMmdbPath() {
        return PathHelper.combine(filesDir, "Country.mmdb");
    }

    public static String getClashConfigPath() {
        return PathHelper.combine(filesDir, "config.yaml");
    }

    public static String getTrojanConfigPath() {
        return PathHelper.combine(filesDir, "config.json");
    }
}
