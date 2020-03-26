package io.github.trojan_gfw.igniter;

import android.app.Application;

import io.github.trojan_gfw.igniter.initializer.InitializerHelper;

public class IgniterApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        InitializerHelper.runInit(this);
    }
}
