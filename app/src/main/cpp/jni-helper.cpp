#include "jni.h"
#include <cstdint>
#include <string>
#include <config.h>
#include <service.h>
#include <n2t/n2t.h>
#include <n2t/n2s.h>
using namespace std;
using namespace Net2Tr;

static void startTrojan(const string &config)
{
    Config c;
    c.load(config);
    Service(c).run();
}

static void startN2S(int tun_fd, const string &ip_addr, const string &netmask, const string &ip6_addr, uint16_t mtu, const string &socks5_addr, uint16_t socks5_port)
{
    N2T n2t(ip_addr, netmask, ip6_addr, mtu);
    N2S(tun_fd, n2t, socks5_addr, socks5_port).start();
}

extern "C" {
    JNIEXPORT void JNICALL Java_io_github_trojan_1gfw_igniter_JNIHelper_trojan(JNIEnv *env, jclass, jstring config) {
        const char *s = env->GetStringUTFChars(config, 0);
        startTrojan(s);
    }

    JNIEXPORT void JNICALL Java_io_github_trojan_1gfw_igniter_JNIHelper_n2s(JNIEnv *env, jclass, jint tun_fd, jstring ip_addr, jstring netmask, jstring ip6_addr, jint mtu, jstring socks5_addr, jint socks5_port) {
        const char *s1 = env->GetStringUTFChars(ip_addr, 0);
        const char *s2 = env->GetStringUTFChars(netmask, 0);
        const char *s3 = env->GetStringUTFChars(ip6_addr, 0);
        const char *s4 = env->GetStringUTFChars(socks5_addr, 0);
        startN2S(tun_fd, s1, s2, s3, uint16_t(mtu), s4, uint16_t(socks5_port));
    }
}

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    return JNI_VERSION_1_6;
}
