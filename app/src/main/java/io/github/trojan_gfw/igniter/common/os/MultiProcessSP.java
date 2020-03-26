package io.github.trojan_gfw.igniter.common.os;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * A SharedPreferences wrapper for multi-process environment.
 */
public class MultiProcessSP {
    private static final String SP_NAME = "MultiProcessSP";
    private static final String FIRST_START = "first_start";
    private static final String ENABLE_CLASH = "enb_clash";
    private static Context sContext;

    public static void init(Context context) {
        sContext = context.getApplicationContext();
    }

    private static SharedPreferences sp() {
        return sContext.getSharedPreferences(SP_NAME, Context.MODE_MULTI_PROCESS);
    }

    private static void setString(String key, String value) {
        sp().edit().putString(key, value).apply();
    }

    private static String getString(String key, String value) {
        return sp().getString(key, value);
    }

    private static void setBoolean(String key, boolean value) {
        sp().edit().putBoolean(key, value).apply();
    }

    private static boolean getBoolean(String key, boolean fallback) {
        return sp().getBoolean(key, fallback);
    }

    /**
     * If it is the first time to start this application.
     *
     * @param fallback default value
     * @return true if this is the first time to start application, false otherwise.
     */
    public static boolean isFirstStart(boolean fallback) {
        return getBoolean(FIRST_START, fallback);
    }

    /**
     * Set if it is the first time to start this application.
     *
     * @param firstStart value
     */
    public static void setIsFirstStart(boolean firstStart) {
        setBoolean(FIRST_START, firstStart);
    }

    /**
     * Get the setting of enabling or disabling clash in proxy.
     *
     * @param fallback default value
     * @return whether enable clash in proxy.
     */
    public static boolean getEnableClash(boolean fallback) {
        return getBoolean(ENABLE_CLASH, fallback);
    }

    /**
     * Set the setting of enabling or disabling clash in proxy.
     *
     * @param value value
     */
    public static void setEnableClash(boolean value) {
        setBoolean(ENABLE_CLASH, value);
    }
}
