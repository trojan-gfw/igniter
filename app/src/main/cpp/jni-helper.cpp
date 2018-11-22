#include "jni.h"
#include <cstdint>
#include <string>
#include <config.h>
#include <service.h>
#include <n2t/n2t.h>
#include <n2t/n2s.h>
using namespace std;
using namespace Net2Tr;

void startTrojan(const string &filename)
{
    Config c;
    c.load(filename);
    Service(c).run();
}

void startN2S(int tun_fd, const string &ip_addr, const string &netmask, const string &ip6_addr, uint16_t mtu, const string &socks5_addr, uint16_t socks5_port)
{
    N2T n2t(ip_addr, netmask, ip6_addr, mtu);
    N2S(tun_fd, n2t, socks5_addr, socks5_port).start();
}

extern "C" {
}

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    return JNI_VERSION_1_6;
}
