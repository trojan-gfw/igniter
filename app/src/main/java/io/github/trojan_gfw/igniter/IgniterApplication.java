package io.github.trojan_gfw.igniter;

import android.app.Application;
import android.content.Context;

import io.github.trojan_gfw.igniter.initializer.InitializerHelper;

public class IgniterApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        InitializerHelper.runInit(this);
    }
}
