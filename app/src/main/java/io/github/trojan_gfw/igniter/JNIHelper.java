package io.github.trojan_gfw.igniter;

public class JNIHelper {
    static {
        System.loadLibrary("jni-helper");
    }
    public static native void trojan(String config);
    public static native void n2s(int tun_fd, String ip_addr, String netmask, String ip6_addr, int mtu, String socks5_addr, int socks5_port);
    public static native void stop();
}
