package io.github.trojan_gfw.igniter.common.constants;

import io.github.trojan_gfw.igniter.BuildConfig;

public abstract class Constants {
    public static final String PREFERENCE_AUTHORITY = BuildConfig.APPLICATION_ID;
    public static final String PREFERENCE_PATH = "preferences";
    public static final String PREFERENCE_URI = "content://" + PREFERENCE_AUTHORITY + "/" + PREFERENCE_PATH;
    public static final String PREFERENCE_KEY_ENABLE_CLASH = "enable_clash";
    public static final String PREFERENCE_KEY_ALLOW_LAN = "allow_lan";
    public static final String PREFERENCE_KEY_FIRST_START = "first_start";
    public static final String PREFERENCE_KEY_PROXY_IN_ALLOW_MODE = "proxy_allow_mode";
}
