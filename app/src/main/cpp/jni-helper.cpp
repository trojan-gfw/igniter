#include "jni.h"
#include <cstdint>
#include <string>
#include <thread>
#include <core/config.h>
#include <core/service.h>
using namespace std;


static thread *trojanThread = nullptr;
static Config *trojanConfig = nullptr;
static Service *trojanService = nullptr;


static void startTrojan(const string &config)
{
    trojanConfig = new Config();
    trojanConfig->load(config);
    trojanService = new Service(*trojanConfig);
    trojanService->run();
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


    JNIEXPORT void JNICALL Java_io_github_trojan_1gfw_igniter_JNIHelper_stop(JNIEnv *env, jclass) {

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
