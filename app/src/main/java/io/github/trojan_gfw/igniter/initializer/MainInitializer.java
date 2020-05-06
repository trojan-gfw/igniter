package io.github.trojan_gfw.igniter.initializer;

import android.content.Context;

import io.github.trojan_gfw.igniter.Globals;

/**
 * Initializer that runs in Main Process (Default process).
 */
public class MainInitializer extends Initializer {

    @Override
    public void init(Context context) {
        Globals.Init(context);
    }

    @Override
    public boolean runsInWorkerThread() {
        return false;
    }
}
