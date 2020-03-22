package io.github.trojan_gfw.igniter.common.os;

import android.content.Context;
import android.content.SharedPreferences;

/**
 *  Application-independent SharedPreferences wrapper. Used for caching application configurations.
 */
public class CommonSP {
    private static final String SP_NAME = "IgniterSP";
    private static final String EXEMPT_APP_LIST = "exempt_app_list";
    private static Context sContext;

    public static void init(Context context) {
        sContext = context;
    }

    private static SharedPreferences sp() {
        return sContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    private static void setString(String key, String value) {
        sp().edit().putString(key, value).apply();
    }

    private static String getString(String key, String value) {
        return sp().getString(key, value);
    }

    /**
     * Get VPN exempt application list configuration.
     *
     * @param fallback default value
     * @return The cached configuration or default value if there's no cache.
     */
    public static String getExemptAppListConfig(String fallback) {
        return getString(EXEMPT_APP_LIST, fallback);
    }

    /**
     * Cache VPN exempt application list configuration.
     *
     * @param config VPN exempt application list configuration
     */
    public static void setExemptAppListConfig(String config) {
        setString(EXEMPT_APP_LIST, config);
    }
}
