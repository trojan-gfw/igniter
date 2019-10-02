#include "jni.h"
#include <cstdint>
#include <string>
#include <thread>
#include <core/config.h>
#include <core/service.h>
#include <n2t/n2t.h>
#include <n2t/n2s.h>
using namespace std;
using namespace Net2Tr;

static thread *trojanThread = nullptr;
static thread *n2sThread = nullptr;
static Config *trojanConfig = nullptr;
static Service *trojanService = nullptr;
static N2T *n2tService = nullptr;
static N2S *n2sService = nullptr;

static void startTrojan(const string &config)
{
    trojanConfig = new Config();
    trojanConfig->load(config);
    trojanService = new Service(*trojanConfig);
    trojanService->run();
}

static void startN2S(int tun_fd, const string &ip_addr, const string &netmask, const string &ip6_addr, uint16_t mtu, const string &socks5_addr, uint16_t socks5_port)
{
    n2tService = new N2T(ip_addr, netmask, ip6_addr, mtu);
    n2sService = new N2S(tun_fd, *n2tService, socks5_addr, socks5_port);
    n2sService->start();
}

extern "C" {
    JNIEXPORT void JNICALL Java_io_github_trojan_1gfw_igniter_JNIHelper_trojan(JNIEnv *env, jclass, jstring config) {
        if (trojanThread != nullptr)
            return;
        const char *s = env->GetStringUTFChars(config, 0);
        string a(s);
        env->ReleaseStringUTFChars(config, s);
        trojanThread = new thread(startTrojan, a);
    }

    JNIEXPORT void JNICALL Java_io_github_trojan_1gfw_igniter_JNIHelper_n2s(JNIEnv *env, jclass, jint tun_fd, jstring ip_addr, jstring netmask, jstring ip6_addr, jint mtu, jstring socks5_addr, jint socks5_port) {
        if (n2sThread != nullptr)
            return;
        const char *s1 = env->GetStringUTFChars(ip_addr, 0);
        const char *s2 = env->GetStringUTFChars(netmask, 0);
        const char *s3 = env->GetStringUTFChars(ip6_addr, 0);
        const char *s4 = env->GetStringUTFChars(socks5_addr, 0);
        string a(s1);
        string b(s2);
        string c(s3);
        string d(s4);
        env->ReleaseStringUTFChars(socks5_addr, s4);
        env->ReleaseStringUTFChars(ip6_addr, s3);
        env->ReleaseStringUTFChars(netmask, s2);
        env->ReleaseStringUTFChars(ip_addr, s1);
        n2sThread = new thread(startN2S, tun_fd, a, b, c, uint16_t(mtu), d, uint16_t(socks5_port));
    }

    JNIEXPORT void JNICALL Java_io_github_trojan_1gfw_igniter_JNIHelper_stop(JNIEnv *env, jclass) {
        if (n2sThread != nullptr) {
            n2sService->stop();
            n2sThread->join();
            delete n2sService;
            delete n2tService;
            delete n2sThread;
            n2sThread = nullptr;
        }
        if (trojanThread != nullptr) {
            trojanService->stop();
            trojanThread->join();
            delete trojanService;
            delete trojanConfig;
            delete trojanThread;
            trojanThread = nullptr;
        }
    }
}

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    return JNI_VERSION_1_6;
}
