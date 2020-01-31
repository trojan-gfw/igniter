package io.github.trojan_gfw.igniter;

public class JNIHelper {
    static {
        System.loadLibrary("jni-helper");
    }

    public static native void trojan(String config);

    public static native void stop();
}
