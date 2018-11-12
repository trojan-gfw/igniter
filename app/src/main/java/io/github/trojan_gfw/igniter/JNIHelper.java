package io.github.trojan_gfw.igniter;

import android.support.annotation.NonNull;
import android.system.ErrnoException;

public class JNIHelper {
    static {
        System.loadLibrary("jni-helper");
    }
    public static native void sendFd(int fd, @NonNull String path) throws ErrnoException;
}
