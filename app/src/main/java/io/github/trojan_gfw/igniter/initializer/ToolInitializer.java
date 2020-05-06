package io.github.trojan_gfw.igniter.initializer;

import android.content.Context;

import io.github.trojan_gfw.igniter.Globals;

/**
 * Initializer that runs in Tools Process.
 */
public class ToolInitializer extends Initializer {

    @Override
    public void init(Context context) {
        Globals.Init(context);
    }

    @Override
    public boolean runsInWorkerThread() {
        return false;
    }
}
