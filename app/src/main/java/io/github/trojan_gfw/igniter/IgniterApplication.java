package io.github.trojan_gfw.igniter;

import android.app.Application;

import io.github.trojan_gfw.igniter.common.os.CommonSP;

public class IgniterApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CommonSP.init(this);
    }
}
