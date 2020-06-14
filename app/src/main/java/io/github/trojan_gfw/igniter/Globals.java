package io.github.trojan_gfw.igniter;

import android.content.Context;
import android.os.Environment;

import java.io.File;

public class Globals {

    private static String cacheDir;
    private static String filesDir;
    private static TrojanConfig trojanConfigInstance;

    public static void Init(Context ctx) {
        cacheDir = ctx.getCacheDir().getAbsolutePath();
        filesDir = ctx.getFilesDir().getAbsolutePath();
        trojanConfigInstance = new TrojanConfig();
        trojanConfigInstance.setCaCertPath(Globals.getCaCertPath());
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

    public static String getTrojanConfigListPath() {
        return PathHelper.combine(filesDir, "config_list.json");
    }

    public static String getIgniterExportPath() {
        return PathHelper.combine(externalFilesDir, "config_list.txt");
    }

    public static String getPreferencesFilePath() {
        return PathHelper.combine(filesDir, "preferences.txt");
    }

    public static String getBlockedAppListPath() {
        return PathHelper.combine(filesDir, "exempted_app_list.txt");
    }

    public static String getAllowedAppListPath() {
        return PathHelper.combine(filesDir, "allow_app_list.txt");
    }

    public static void setTrojanConfigInstance(TrojanConfig config) {
        trojanConfigInstance = config;
    }

    public static TrojanConfig getTrojanConfigInstance() {
        return trojanConfigInstance;
    }
}
