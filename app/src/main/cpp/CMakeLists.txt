cmake_minimum_required(VERSION 3.6)
project(igniter)
find_library( # Sets the name of the path variable.
          androidLogLib

          # Specifies the name of the NDK library that
          # you want CMake to locate.
          log)
add_library(trojan
    trojan/src/authenticator.cpp
    trojan/src/clientsession.cpp
    trojan/src/config.cpp
    trojan/src/forwardsession.cpp
    trojan/src/log.cpp
    trojan/src/serversession.cpp
    trojan/src/service.cpp
    trojan/src/session.cpp
    trojan/src/socks5address.cpp
    trojan/src/ssldefaults.cpp
    trojan/src/sslsession.cpp
    trojan/src/trojanrequest.cpp
    trojan/src/udppacket.cpp
    trojan/src/version.cpp)
target_include_directories(trojan PRIVATE
    ${CMAKE_SOURCE_DIR}/libs/include
    ${CMAKE_SOURCE_DIR}/libs/include/${ANDROID_ABI})
target_link_libraries(trojan
    ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libboost_program_options.a
    ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libboost_system.a
    ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libssl.a
    ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libcrypto.a)
add_library(n2t
    libn2t/src/n2t/n2s.cpp
    libn2t/src/n2t/n2t.cpp
    libn2t/src/n2t/socket.cpp
    libn2t/src/n2t/tcpsession.cpp
    libn2t/src/n2t/udpsession.cpp
    libn2t/src/n2t/utils.cpp
    libn2t/src/custom/sys_arch.c
    libn2t/src/lwip/src/core/init.c
    libn2t/src/lwip/src/core/def.c
    libn2t/src/lwip/src/core/dns.c
    libn2t/src/lwip/src/core/inet_chksum.c
    libn2t/src/lwip/src/core/ip.c
    libn2t/src/lwip/src/core/mem.c
    libn2t/src/lwip/src/core/memp.c
    libn2t/src/lwip/src/core/netif.c
    libn2t/src/lwip/src/core/pbuf.c
    libn2t/src/lwip/src/core/raw.c
    libn2t/src/lwip/src/core/stats.c
    libn2t/src/lwip/src/core/sys.c
    libn2t/src/lwip/src/core/altcp.c
    libn2t/src/lwip/src/core/altcp_alloc.c
    libn2t/src/lwip/src/core/altcp_tcp.c
    libn2t/src/lwip/src/core/tcp.c
    libn2t/src/lwip/src/core/tcp_in.c
    libn2t/src/lwip/src/core/tcp_out.c
    libn2t/src/lwip/src/core/timeouts.c
    libn2t/src/lwip/src/core/udp.c
    libn2t/src/lwip/src/core/ipv4/autoip.c
    libn2t/src/lwip/src/core/ipv4/dhcp.c
    libn2t/src/lwip/src/core/ipv4/etharp.c
    libn2t/src/lwip/src/core/ipv4/icmp.c
    libn2t/src/lwip/src/core/ipv4/igmp.c
    libn2t/src/lwip/src/core/ipv4/ip4_frag.c
    libn2t/src/lwip/src/core/ipv4/ip4.c
    libn2t/src/lwip/src/core/ipv4/ip4_addr.c
    libn2t/src/lwip/src/core/ipv6/dhcp6.c
    libn2t/src/lwip/src/core/ipv6/ethip6.c
    libn2t/src/lwip/src/core/ipv6/icmp6.c
    libn2t/src/lwip/src/core/ipv6/inet6.c
    libn2t/src/lwip/src/core/ipv6/ip6.c
    libn2t/src/lwip/src/core/ipv6/ip6_addr.c
    libn2t/src/lwip/src/core/ipv6/ip6_frag.c
    libn2t/src/lwip/src/core/ipv6/mld6.c
    libn2t/src/lwip/src/core/ipv6/nd6.c)
target_include_directories(n2t PRIVATE
    ${CMAKE_SOURCE_DIR}/libn2t/src/custom
    ${CMAKE_SOURCE_DIR}/libn2t/src/lwip/src/include
    ${CMAKE_SOURCE_DIR}/libs/include)
target_link_libraries(n2t
    ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libboost_system.a
    ${androidLogLib})
add_library(jni-helper SHARED jni-helper.cpp)
target_include_directories(jni-helper PRIVATE
    ${CMAKE_SOURCE_DIR}/trojan/src
    ${CMAKE_SOURCE_DIR}/libn2t/src
    ${CMAKE_SOURCE_DIR}/libs/include
    ${CMAKE_SOURCE_DIR}/libs/include/${ANDROID_ABI})
target_link_libraries(jni-helper trojan n2t)
