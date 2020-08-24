package io.github.trojan_gfw.igniter.common.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;

public class DisplayUtils {
    public static int getScreenWidth() {
        Resources resources = Resources.getSystem();
        DisplayMetrics dm = resources.getDisplayMetrics();
        return dm.widthPixels;
    }
}
