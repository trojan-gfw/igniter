package io.github.trojan_gfw.igniter;

public class JNIHelper {

    public static void trojan(String config) {
        trojan.Trojan.runClient(config);
    }

    public static void stop() {
        trojan.Trojan.stopClient();
    }
}
