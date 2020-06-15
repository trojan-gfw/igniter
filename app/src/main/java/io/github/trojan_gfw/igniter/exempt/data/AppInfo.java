package io.github.trojan_gfw.igniter.exempt.data;

import android.graphics.drawable.Drawable;

import java.util.Optional;

public class AppInfo implements Cloneable, Comparable<AppInfo> {
    private String appName;
    private String appNameInLowercase;
    private Drawable icon;
    private String packageName;
    private boolean exempt;

    public String getAppName() {
        return appName;
    }

    public String getAppNameInLowercase() {
        if (appNameInLowercase == null) {
            // lazy loading.
            appNameInLowercase = appName.toLowerCase();
        }
        return appNameInLowercase;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public boolean isExempt() {
        return exempt;
    }

    public void setExempt(boolean exempt) {
        this.exempt = exempt;
    }

    @Override
    public int compareTo(AppInfo o) {
        if (exempt ^ o.exempt) {
            return exempt ? -1 : 1;
        }
        if (appName == null) {
            return "".compareTo(o.appName);
        }
        return appName.compareTo(o.appName);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        AppInfo appInfo = (AppInfo) super.clone();
        appInfo.appName = appName;
        appInfo.icon = icon;
        appInfo.packageName = packageName;
        appInfo.exempt = exempt;
        return appInfo;
    }
}
